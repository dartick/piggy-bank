package org.xiaoheshan.piggy.bank.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author _Chf
 * @since 05-10-2018
 */
@Component
public class RedisDistLock {

    private final Logger logger = LoggerFactory.getLogger(RedisDistLock.class);

    private static final String REDIS_LOCK_KEY_PREFIX = "redis-lock-";
    private static final ThreadLocal<String> UID_HOLDER = ThreadLocal.withInitial(() -> UUID.randomUUID().toString());
    private static final Random rnd = new Random();

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<String> lockScript = new DefaultRedisScript<String>("return redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2])", String.class);
    private final RedisScript<Boolean> unLockScript = new DefaultRedisScript<Boolean>("if redis.call('GET', KEYS[1]) == ARGV[1] then redis.call('DEL', KEYS[1]) return true else return false end", Boolean.class);

    @Autowired
    public RedisDistLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void lock(String key, long expire, TimeUnit unit) {
        for (;;) {
            if (tryLock(key, expire, unit)) {
                return;
            }
            int max = 500000000;
            int min = 2000000;
            LockSupport.parkNanos(rnd.nextInt(max) % (max - min + 1) + max);
        }
    }

    public boolean tryLock(String key, long expire, TimeUnit unit) {
        String isSucceed = redisTemplate.execute(lockScript, Collections.singletonList(REDIS_LOCK_KEY_PREFIX + key), UID_HOLDER.get(), String.valueOf(unit.toMillis(expire)));
        if ("OK".equals(isSucceed)) {
            return true;
        }
        UID_HOLDER.remove();
        return false;
    }

    public void unlock(String key) {
        try {
            redisTemplate.execute(unLockScript, Collections.singletonList(REDIS_LOCK_KEY_PREFIX + key), UID_HOLDER.get());
        } finally {
            UID_HOLDER.remove();
        }
    }
}
