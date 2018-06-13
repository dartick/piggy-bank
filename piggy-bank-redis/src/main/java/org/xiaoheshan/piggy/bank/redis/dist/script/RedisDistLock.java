package org.xiaoheshan.piggy.bank.redis.dist.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author _Chf
 * @since 05-10-2018
 */
@Component
public class RedisDistLock {

    private final Logger logger = LoggerFactory.getLogger(RedisDistLock.class);

    private static final String REDIS_LOCK_KEY_PREFIX = "application-code:redis:lock:";
    private static final ThreadLocal<String> UID_HOLDER = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());
    private static final Random RND = new Random();

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<String> lockScript = new DefaultRedisScript<String>("return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2])", String.class);
    private final RedisScript<Boolean> unLockScript = new DefaultRedisScript<Boolean>("if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]) return true else return false end", Boolean.class);

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
            String isSucceed = redisTemplate.execute(lockScript, Collections.singletonList(REDIS_LOCK_KEY_PREFIX + key), UID_HOLDER.get(), String.valueOf(unit.toMillis(expire)));
            if ("OK".equals(isSucceed)) {
                return true;
            }
        } catch (Exception e) {
            UID_HOLDER.remove();
            throw new RuntimeException(e);
        }
        UID_HOLDER.remove();
        return false;
    }

    public void unlock(String key) {
        Assert.notNull(key, "key must not be null");
        try {
            redisTemplate.execute(unLockScript, Collections.singletonList(REDIS_LOCK_KEY_PREFIX + key), UID_HOLDER.get());
        } finally {
            UID_HOLDER.remove();
        }
    }
}
