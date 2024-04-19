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

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 非对称缓存密钥
 */
@Slf4j
@Component
public class AsymmetryCacheKey extends CacheKey {

    private volatile String publicKey;

    private volatile String privateKey;

    // 本地最新密钥版本，
    private final AtomicLong version = new AtomicLong(0);

    // 历史版本密钥记录map<version, key>
    private final ConcurrentHashMap<Long, AsymmetryKeyWrapper> historyKeyMap = new ConcurrentHashMap<>();

    // 是否为刷新非对称密钥的节点
    private volatile boolean isAsymkRefreshNode = false;

    private final ReentrantReadWriteLock asymkLock = new ReentrantReadWriteLock();

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public AsymmetryCacheKey(RedisConnectionFactory redisConnectionFactory) {
        super(Collections.singletonList(new ChannelTopic(HenanRedisConstants.ASYMMETRY_KEY_SYNC_TOPIC)), redisConnectionFactory);
    }

    /**
     * 接受密钥同步消息
     * @param message 消息体
     * @param pattern 订阅的topic名称
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        AsymmetryKeyWrapper newKey = JSONObject.parseObject(message.getBody(), AsymmetryKeyWrapper.class);
        log.info("接收到非对称密钥同步订阅消息:{}", new String(message.getBody()));
        if (null == newKey) {
            // todo 主动获取密钥
            throw new CryptoException("非对称密钥不存在");
        }
        if (isAsymkRefreshNode) {
            // 如果是当前节点刷新的密钥，则不再更新本地信息
            isAsymkRefreshNode = false;
            return;
        }
        this.refreshLocal(newKey);
    }

    /**
     * 更新本地密钥
     */
    @Override
    public void refreshLocal(KeyWrapper keyWrapper) {
        AsymmetryKeyWrapper oldKey = this.writeSafely((AsymmetryKeyWrapper) keyWrapper);
        this.clearExpiredKey();
        if (null != oldKey && oldKey.getVersion() > 0) {
            this.historyKeyMap.putIfAbsent(oldKey.getVersion(), oldKey);
        }
        log.info("当前历史非对称密钥可用版本:{}", historyKeyMap.keySet());
    }

    /**
     * 清除过期密钥
     */
    protected void clearExpiredKey() {
        this.historyKeyMap.entrySet().removeIf(element -> {
            boolean expired = element.getValue().getExpireTime() < System.currentTimeMillis();
            if (expired) {
                log.info("移除过期的版本{}非对称密钥:{}", element.getKey(), JSON.toJSONString(element.getValue()));
            }
            return expired;
        });
    }

    /**
     * 并发安全写入本地密钥
     * @param newKey 如果为null, 则表示需要创建密钥
     * @return AsymmetryKeyWrapper 返回前一个版本的密钥
     */
    private AsymmetryKeyWrapper writeSafely(AsymmetryKeyWrapper newKey) {
        log.info("更新本地非对称密钥:{}", JSON.toJSONString(newKey));
        AsymmetryKeyWrapper oldKeyWrapper = null;
        try {
            boolean isLocked = this.asymkLock.writeLock().tryLock(30 * 1000, TimeUnit.MILLISECONDS);
            if (isLocked) {

                String oldPubKey = this.publicKey;
                String oldPrvKey = this.privateKey;
                long oldVersion = this.version.get();
                oldKeyWrapper = new AsymmetryKeyWrapper(oldPubKey, oldPrvKey, oldVersion, System.currentTimeMillis() + HISTORY_EXPIRE);

                if (null == newKey) {
                    KeyPair keyPair = WebCryptoSupport.genKeyPair();
                    newKey = new AsymmetryKeyWrapper(keyPair.getPublicKey(), keyPair.getPrivateKey(), this.version.incrementAndGet());
                }
                this.version.set(newKey.getVersion());
                this.publicKey = newKey.getKeyPair().getPublicKey();
                this.privateKey = newKey.getKeyPair().getPrivateKey();
            }
        } catch (InterruptedException | NoSuchAlgorithmException e) {
            log.error("非对称密钥本地写入失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("请求处理失败");
        } finally {
            if (this.asymkLock.isWriteLockedByCurrentThread()) {
                this.asymkLock.writeLock().unlock();
            }
        }
        return oldKeyWrapper;
    }

    /**
     * 并发安全读取本地密钥
     * @return AsymmetryKeyWrapper
     */
    private AsymmetryKeyWrapper readSafely() {
        try {
            boolean isLocked = this.asymkLock.readLock().tryLock(5 * 1000, TimeUnit.MILLISECONDS);
            if (isLocked) {
                return new AsymmetryKeyWrapper(this.publicKey, this.privateKey, this.version.get());
            }
        } catch (InterruptedException e) {
            log.error("读取本地非对称密钥失败:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("请求处理失败");
        } finally {
            this.asymkLock.readLock().unlock();
        }
        return null;
    }

    /**
     * 获取特定版本的密钥
     * @param reqVersion 请求版本
     * @param locator 密钥定位符
     * @return AsymmetryKeyWrapper
     */
    public AsymmetryKeyWrapper getKey(long reqVersion, String locator) {
        AsymmetryKeyWrapper curKey = this.readSafely();
        if (null == curKey) {
            return null;
        }
        log.info("获取非对称密钥, 请求版本:{}, 本地版本:{}", reqVersion, curKey.getVersion());
        if (reqVersion == curKey.getVersion() || curKey.getVersion() == 0) {
            String curPubKey = curKey.getKeyPair().getPublicKey();
            String curPrvKey = curKey.getKeyPair().getPrivateKey();
            AsymmetryKeyWrapper newKey = (AsymmetryKeyWrapper) this.refreshCache(locator);
            // 如果请求版本的密钥存在，则使用请求版本的密钥进行解密
            if (StringUtils.isBlank(curPubKey) || StringUtils.isBlank(curPrvKey)) {
                return newKey;
            }
            return new AsymmetryKeyWrapper(curPubKey, curPrvKey, curKey.getVersion());
        } else {
            // 如果请求版本与本地版本不一致，则从历史记录中获取
            log.info("尝试获取历史版本非对称密钥, 当前本地历史非对称密钥版本:{}", historyKeyMap.keySet());
            AsymmetryKeyWrapper keyWrapper = historyKeyMap.get(reqVersion);
            if (null != keyWrapper) {
                long now = System.currentTimeMillis();
                if (now > keyWrapper.getExpireTime()) {
                    log.info("本地版本{}非对称密钥已过期", keyWrapper.getVersion());
                    return null;
                }
            } else {
                log.info("尝试获取最新版本非对称密钥");
                keyWrapper = this.getKeyFromCache(locator);
                if (keyWrapper == null || reqVersion != keyWrapper.getVersion()) {
                    log.info("未获取到对应版本{}的非对称密钥", reqVersion);
                    return null;
                }
            }
            return keyWrapper;
        }
    }

    /**
     * 获取当前最新版本密钥
     * @return AsymmetryKeyWrapper
     */
    public AsymmetryKeyWrapper getLatestKey() {
        return this.readSafely();
    }

    /**
     * 从缓存中获取密钥
     */
    protected AsymmetryKeyWrapper getKeyFromCache(String locator) {
        String cacheKey = stringRedisTemplate.opsForValue().get(locator);
        return StringUtils.isBlank(cacheKey) ? null : JSONObject.parseObject(cacheKey, AsymmetryKeyWrapper.class);
    }

    /**
     * 刷新密钥缓存，并发送同步消息
     */
    @Override
    protected KeyWrapper refreshCache(String locator) {
        AsymmetryKeyWrapper cacheKeyWrapper = this.getKeyFromCache(locator);
        log.info("第一次获取缓存非对称密钥");
        if (null == cacheKeyWrapper) {
            RedisLock redisLock = new RedisLock(redisTemplate, HenanRedisConstants.ASYMMETRY_KEY_UPDATE_LOCK);
            boolean isLocked = this.getRefreshLock(redisLock);
            if (isLocked) {
                try {
                    // 二次校验，防止高并发情况下重复刷新缓存密钥
                    cacheKeyWrapper = this.getKeyFromCache(locator);
                    log.info("第二次获取缓存非对称密钥");
                    if (null == cacheKeyWrapper) {
                        log.info("非对称密钥缓存为空，刷新缓存并同步其他节点");
                        // 生成新对称密钥，刷新缓存，并同步其他节点
                        AsymmetryKeyWrapper oldKeyWrapper = this.writeSafely(null);
                        if (oldKeyWrapper.getVersion() > 0) {
                            // 如果不是初始版本，则需要记录密钥历史
                            this.historyKeyMap.putIfAbsent(oldKeyWrapper.getVersion(), oldKeyWrapper);
                            log.info("更新本地密钥历史记录{}", oldKeyWrapper);
                        }
                        cacheKeyWrapper = this.readSafely();
                        String keyJson = JSON.toJSONString(cacheKeyWrapper);
                        // 发送密钥同步消息
                        isAsymkRefreshNode = true;
                        this.publish(HenanRedisConstants.ASYMMETRY_KEY_SYNC_TOPIC, keyJson);
                        Boolean res = stringRedisTemplate.opsForValue().setIfAbsent(locator, keyJson);
                        // todo 如果执行到此处时系统停止运行，缓存密钥则不会有过期时间
                        if (Boolean.TRUE.equals(res)) {
                            stringRedisTemplate.expire(locator, 7 * 24, TimeUnit.HOURS);
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
    @Bean("asymmetryCacheKeySubscribe")
    public RedisMessageListenerContainer subscribe() {
        log.info("非对称密钥同步消息订阅");
        return super.subscribe();
    }

    @Override
    public void publish(String topic, String message) {
        log.info("发布非对称密钥同步消息, topic:{}, message:{}", topic, message);
        stringRedisTemplate.convertAndSend(topic, message);
    }

    protected boolean getRefreshLock(RedisLock redisLock) {
        boolean isLocked = false;
        try {
            // 重试次数
            int retryCount = 1;
            while (!isLocked && retryCount <= 5) {
                log.info("第{}次尝试获取非对称密钥同步锁{}", retryCount, HenanRedisConstants.ASYMMETRY_KEY_UPDATE_LOCK);
                isLocked = redisLock.lock();
                Thread.sleep(1000);
                retryCount++;
            }
        } catch (InterruptedException e) {
            log.error("非对称密钥刷新缓存获取redis锁异常:\n{}", ExceptionUtils.getFullStackTrace(e));
            throw new CryptoException("刷新异常");
        }
        return isLocked;
    }

}
