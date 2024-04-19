package com.chinaums.spread.common.crypto.key;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chinaums.spread.common.constant.HenanRedisConstants;
import com.chinaums.spread.common.crypto.WebCryptoSupport;
import com.chinaums.spread.common.crypto.exception.CryptoException;
import com.chinaums.spread.core.redis.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 对称缓存密钥
 */
@Slf4j
@Component
public class SymmetryCacheKey extends CacheKey {

    // 对称密钥
    private volatile String key;

    // 本地最新密钥版本，
    private final AtomicLong version = new AtomicLong(0);

    // 历史版本密钥记录map<version, key>
    private final ConcurrentHashMap<Long, SymmetryKeyWrapper> historyKeyMap = new ConcurrentHashMap<>();

    private final ReentrantReadWriteLock symkLock = new ReentrantReadWriteLock();
    
    // 是否为刷新对称密钥的节点
    private volatile boolean isSymkRefreshNode = false;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public SymmetryCacheKey(RedisConnectionFactory redisConnectionFactory) {
        super(Collections.singletonList(new ChannelTopic(HenanRedisConstants.SYMMETRY_KEY_SYNC_TOPIC)), redisConnectionFactory);
    }

    /**
     * 接受密钥同步消息
     * @param message 消息体
     * @param pattern 订阅的topic名称
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        SymmetryKeyWrapper newKey = JSONObject.parseObject(message.getBody(), SymmetryKeyWrapper.class);
        log.info("接收到对称密钥同步订阅消息:{}", new String(message.getBody()));
        if (null == newKey) {
            // todo 主动获取密钥
            throw new CryptoException("对称密钥不存在");
        }
        if (isSymkRefreshNode) {
            // 如果是当前节点刷新的密钥，则不再更新本地信息
            isSymkRefreshNode = false;
            return;
        }
        this.refreshLocal(newKey);
    }

    /**
     * 更新本地密钥
     */
    @Override
    public void refreshLocal(KeyWrapper keyWrapper) {
        SymmetryKeyWrapper oldKey = this.writeSafely((SymmetryKeyWrapper) keyWrapper);
        this.clearExpiredKey();
        if (null != oldKey && oldKey.getVersion() > 0) {
            this.historyKeyMap.putIfAbsent(oldKey.getVersion(), oldKey);
        }
        log.info("当前历史对称密钥可用版本:{}", historyKeyMap.keySet());
    }

    /**
     * 清除过期密钥
     */
    protected void clearExpiredKey() {
        this.historyKeyMap.entrySet().removeIf(element -> {
            boolean expired = element.getValue().getExpireTime() < System.currentTimeMillis();
            if (expired) {
                log.info("移除过期的版本{}对称密钥:{}", element.getKey(), JSON.toJSONString(element.getValue()));
            }
            return expired;
        });
    }

    /**
     * 并发安全写入本地密钥
     * @param newKey 如果为null, 则表示需要创建密钥
     * @return SymmetryKeyWrapper 返回前一个版本的密钥
     */
    private SymmetryKeyWrapper writeSafely(SymmetryKeyWrapper newKey) {
        log.info("更新本地对称密钥:{}", JSON.toJSONString(newKey));
        SymmetryKeyWrapper oldKeyWrapper = null;
        try {
            boolean isLocked = this.symkLock.writeLock().tryLock(1000, TimeUnit.MILLISECONDS);
            if (isLocked) {
                String oldKey = this.key;
                long oldVersion = this.version.get();
                oldKeyWrapper = new SymmetryKeyWrapper(oldKey, oldVersion, System.currentTimeMillis() + HISTORY_EXPIRE);

                if (null == newKey) {
                    newKey = new SymmetryKeyWrapper(WebCryptoSupport.generate16ByteRandom(), this.version.incrementAndGet());
                }
                this.version.set(newKey.getVersion());
                this.key = newKey.getSecretKey();
            }
        } catch (InterruptedException e) {
            log.error("对称密钥本地写入失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("请求处理失败");
        } finally {
            if (this.symkLock.isWriteLockedByCurrentThread()) {
                this.symkLock.writeLock().unlock();
            }
        }
        return oldKeyWrapper;
    }

    /**
     * 并发安全读取本地密钥
     * @return SymmetryKeyWrapper
     */
    private SymmetryKeyWrapper readSafely() {
        try {
            boolean isLocked = symkLock.readLock().tryLock(100, TimeUnit.MILLISECONDS);
            if (isLocked) {
                return new SymmetryKeyWrapper(this.key, this.version.get());
            }
        } catch (InterruptedException e) {
            log.error("读取本地对称密钥失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("请求处理失败");
        } finally {
            symkLock.readLock().unlock();
        }
        return null;
    }

    /**
     * 获取特定版本的密钥
     * @param reqVersion 请求版本
     * @param locator 密钥定位符
     * @return SymmetryKeyWrapper
     */
    public SymmetryKeyWrapper getKey(long reqVersion, String locator) {
        SymmetryKeyWrapper curKeyWrapper = this.readSafely();
        if (null == curKeyWrapper) {
            return null;
        }
        log.info("获取对称密钥, 请求版本:{}, 本地版本:{}", reqVersion, curKeyWrapper.getVersion());
        if (reqVersion == curKeyWrapper.getVersion() || curKeyWrapper.getVersion() == 0) {
            String curKey = curKeyWrapper.getSecretKey();
            SymmetryKeyWrapper newKey = (SymmetryKeyWrapper) this.refreshCache(locator);
            // 如果请求版本的密钥存在，则使用请求版本的密钥进行解密
            return StringUtils.isBlank(curKey) ? newKey : new SymmetryKeyWrapper(curKey, this.version.get());
        } else {
            // 如果请求版本与本地版本不一致，则从历史记录中获取
            log.info("尝试获取历史版本对称密钥, 当前本地历史对称密钥版本:{}", historyKeyMap.keySet());
            SymmetryKeyWrapper keyWrapper = historyKeyMap.get(reqVersion);
            if (null != keyWrapper) {
                long now = System.currentTimeMillis();
                if (now > keyWrapper.getExpireTime()) {
                    log.info("本地版本{}对称密钥已过期", keyWrapper.getVersion());
                    return null;
                }
            } else {
                log.info("尝试获取最新版本对称密钥");
                keyWrapper = this.getKeyFromCache(locator);
                if (keyWrapper == null || reqVersion != keyWrapper.getVersion()) {
                    log.info("未获取到对应版本{}的对称密钥", reqVersion);
                    return null;
                }
            }
            return keyWrapper;
        }
    }

    /**
     * 从缓存中获取密钥
     */
    protected SymmetryKeyWrapper getKeyFromCache(String locator) {
        String cacheKey = stringRedisTemplate.opsForValue().get(locator);
        return StringUtils.isBlank(cacheKey) ? null : JSONObject.parseObject(cacheKey, SymmetryKeyWrapper.class);
    }

    /**
     * 获取当前最新版本密钥
     * @return SymmetryKeyWrapper
     */
    public SymmetryKeyWrapper getLatestKey() {
        return this.readSafely();
    }

    /**
     * 刷新密钥缓存，并发送同步消息
     */
    @Override
    public KeyWrapper refreshCache(String locator) {
        SymmetryKeyWrapper cacheKeyWrapper = this.getKeyFromCache(locator);
        log.info("第一次获取缓存对称密钥");
        if (null == cacheKeyWrapper) {
            RedisLock redisLock = new RedisLock(redisTemplate, HenanRedisConstants.SYMMETRY_KEY_UPDATE_LOCK);
            boolean isLocked = this.getRefreshLock(redisLock);
            if (isLocked) {
                try {
                    // 二次校验，防止高并发情况下重复刷新缓存密钥
                    cacheKeyWrapper = this.getKeyFromCache(locator);
                    log.info("第二次获取缓存对称密钥");
                    if (null == cacheKeyWrapper) {
                        log.info("对称密钥缓存为空，刷新缓存并同步其他节点");
                        // 生成新对称密钥，刷新缓存，并同步其他节点
                        SymmetryKeyWrapper oldKeyWrapper = this.writeSafely(null);
                        if (oldKeyWrapper.getVersion() > 0) {
                            // 如果不是初始版本，则需要记录密钥历史
                            this.historyKeyMap.putIfAbsent(oldKeyWrapper.getVersion(), oldKeyWrapper);
                            log.info("更新本地密钥历史记录{}", oldKeyWrapper);
                        }
                        cacheKeyWrapper = this.readSafely();
                        String keyJson = JSON.toJSONString(cacheKeyWrapper);
                        // 发送密钥同步消息
                        isSymkRefreshNode = true;
                        this.publish(HenanRedisConstants.SYMMETRY_KEY_SYNC_TOPIC, keyJson);
                        Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(locator, keyJson);
                        // todo 如果执行到此处时系统停止运行，缓存密钥则不会有过期时间
                        if (Boolean.TRUE.equals(res)) {
                            stringRedisTemplate.expire(locator, 24, TimeUnit.HOURS);
                        }
                    }
                } finally {
                    redisLock.unlock();
                }
            }
        } else {
            if (this.version.get() == 0) {
                // 节点重启后，本地密钥信息会重置，重新获取密钥后，更新本地密钥信息
                this.writeSafely(cacheKeyWrapper);
            }
        }
        return cacheKeyWrapper;
    }

    public long getVersion() {
        return this.version.get();
    }

    @Override
    @Bean("symmetryCacheKeySubscribe")
    public RedisMessageListenerContainer subscribe() {
        log.info("对称密钥同步消息订阅");
        return super.subscribe();
    }

    @Override
    public void publish(String topic, String message) {
        log.info("发布对称密钥同步消息, topic:{}, message:{}", topic, message);
        stringRedisTemplate.convertAndSend(topic, message);
    }

    protected boolean getRefreshLock(RedisLock redisLock) {
        boolean isLocked = false;
        try {
            // 重试次数
            int retryCount = 1;
            while (!isLocked && retryCount <= 5) {
                log.info("第{}次尝试获取对称密钥同步锁{}", retryCount, HenanRedisConstants.SYMMETRY_KEY_UPDATE_LOCK);
                isLocked = redisLock.lock();
                Thread.sleep(1000);
                retryCount++;
            }
        } catch (InterruptedException e) {
            log.error("对称密钥刷新缓存获取redis锁异常:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("刷新异常");
        }
        return isLocked;
    }
}
