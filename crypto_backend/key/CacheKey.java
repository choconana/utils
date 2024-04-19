package com.chinaums.spread.common.crypto.key;

import com.chinaums.spread.common.redis.message.RedisPublisher;
import com.chinaums.spread.common.redis.message.RedisSubscriber;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;

import java.util.List;

public abstract class CacheKey extends RedisSubscriber implements RedisPublisher  {

    // 历史密钥有效期
    protected final long HISTORY_EXPIRE = 30 * 60 * 1000;

    abstract void refreshLocal(KeyWrapper keyWrapper);

    abstract KeyWrapper refreshCache(String locator);

    public CacheKey(List<ChannelTopic> topicList, RedisConnectionFactory redisConnectionFactory) {
        super(topicList, redisConnectionFactory);
    }

}
