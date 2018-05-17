package org.xiaoheshan.piggy.bank.redis.dist.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author _Chf
 * @since 05-17-2018
 */
@Component
public class RedisDistLock {

    private final Logger logger = LoggerFactory.getLogger(org.xiaoheshan.piggy.bank.redis.dist.script.RedisDistLock.class);

    private static final String REDIS_LOCK_KEY_PREFIX = "redis-lock-";
    private static final ThreadLocal<String> UID_HOLDER = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());
    private static final Random rnd = new Random();

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisDistLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void lock(String key, long expire, TimeUnit unit) {
        for (;;) {
            if (tryLock(key, expire, unit)) {
                return;
            }
            /* 最大等待时长500ms */
            int max = 500000000;
            /* 最小等待时长2ms */
            int min = 2000000;
            /* 使用随机时长，防止同时唤醒导致再次等待 */
            LockSupport.parkNanos(rnd.nextInt(max) % (max - min + 1) + max);
        }
    }

    public boolean tryLock(String key, long expire, TimeUnit unit) {
        try {
            Boolean isSucceed = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                Jedis jedis = (Jedis) connection.getNativeConnection();
                String result = jedis.set(key, UID_HOLDER.get(), "NX", "PX", unit.toMillis(expire));
                return "OK".equals(result);
            });
            if (Optional.ofNullable(isSucceed).orElse(false)) {
                return true;
            }
        } catch (Exception e) {
            UID_HOLDER.remove();
            throw new RuntimeException(e);
        }
        UID_HOLDER.remove();
        return false;
    }

    @SuppressWarnings("unchecked")
    public void unlock(String key) {
        try {
            Boolean isSucceed = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                public <K, V> Boolean execute(RedisOperations<K, V> operations) throws DataAccessException {
                    redisTemplate.watch(key);
                    String uuid = redisTemplate.opsForValue().get(key);
                    if (UID_HOLDER.get().equals(uuid)) {
                        redisTemplate.multi();
                        redisTemplate.delete(key);
                    }
                    List<Object> result = redisTemplate.exec();
                    return !CollectionUtils.isEmpty(result);
                }
            });
            if (!isSucceed) {
                logger.warn("redis dist lock release failed: {} has been changed.", key);
            }
        } finally {
            UID_HOLDER.remove();
        }
    }
}
