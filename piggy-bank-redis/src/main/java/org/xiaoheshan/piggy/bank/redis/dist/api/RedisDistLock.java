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
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author _Chf
 * @since 05-17-2018
 */
@Component
public class RedisDistLock {

    private final Logger logger = LoggerFactory.getLogger(RedisDistLock.class);

    private static final String REDIS_LOCK_KEY_PREFIX = "application-code:redis:lock:";
    private static final ThreadLocal<String> UID_HOLDER = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());
    private static final Random RND = new Random();

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public RedisDistLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T sync(String key, Callable<T> callable) {
        T result = null;
        try {
            this.lock(key);
            result = callable.call();
        } catch (Exception e) {
            logger.error("sync failed in redis distributed lock.", e);
        } finally {
            this.unlock(key);
        }
        return result;
    }

    public <T> T sync(String key, long expire, TimeUnit unit, Callable<T> callable) {
        T result = null;
        try {
            this.lock(key, expire, unit);
            result = callable.call();
        } catch (Exception e) {
            logger.error("sync failed in redis distributed lock.", e);
        } finally {
            this.unlock(key);
        }
        return result;
    }

    public void lock(String key) {
        this.lock(key, 30, TimeUnit.SECONDS);
    }

    public void lock(String key, long expire, TimeUnit unit) {
        for (;;) {
            if (tryLock(key, expire, unit)) {
                return;
            }
            /* 最大等待时长500ms */
            int max = 500_000_000;
            /* 最小等待时长2ms */
            int min = 2_000_000;
            /* 使用随机时长，避免惊群效应 */
            LockSupport.parkNanos(RND.nextInt(max) % (max - min + 1) + max);
        }
    }

    public boolean tryLock(String key, long expire, TimeUnit unit) {
        Assert.notNull(key, "key must not be null");
        Assert.notNull(unit, "unit must not be null");
        try {
            Boolean isSucceed = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
                Jedis jedis = (Jedis) connection.getNativeConnection();
                String result = jedis.set(REDIS_LOCK_KEY_PREFIX + key, UID_HOLDER.get(), "NX", "PX", unit.toMillis(expire));
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
        Assert.notNull(key, "key must not be null");
        try {
            Boolean isSucceed = redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                public <K, V> Boolean execute(RedisOperations<K, V> operations) throws DataAccessException {
                    redisTemplate.watch(REDIS_LOCK_KEY_PREFIX + key);
                    String uuid = redisTemplate.opsForValue().get(REDIS_LOCK_KEY_PREFIX + key);
                    if (UID_HOLDER.get().equals(uuid)) {
                        redisTemplate.multi();
                        redisTemplate.delete(REDIS_LOCK_KEY_PREFIX + key);
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
