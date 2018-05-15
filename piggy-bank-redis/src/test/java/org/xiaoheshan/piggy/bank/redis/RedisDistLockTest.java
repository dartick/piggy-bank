package org.xiaoheshan.piggy.bank.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author _Chf
 * @since 05-11-2018
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class RedisDistLockTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisDistLock redisDistLock;

    @Before
    public void setUp() throws Exception {
        System.out.println("================ 测试开始 ================");
        this.redisTemplate.execute(new DefaultRedisScript<Void>("redis.call('flushall')", Void.class),null);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("================ 测试结束 ================");
    }

    @Test
    public void testRedis() {
        redisTemplate.opsForValue().set("test", "123456", 30, TimeUnit.SECONDS);
    }

    @Test
    public void testNormal() throws Exception {
        int threadCount = 8;
        String key = "testNormal";
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    startSignal.await();
                    try {
                        System.out.println("我是线程" + finalI + " , 我要锁.");
                        redisDistLock.lock(key, 5, TimeUnit.SECONDS);
                        System.out.println("我是线程" + finalI + " , 我拿到锁啦.");
                        Thread.sleep(1000);
                    } finally {
                        redisDistLock.unlock(key);
                        System.out.println("我是线程" + finalI + " , 扔了吧, 没用.");
                    }
                    finishSignal.countDown();
                } catch (InterruptedException ignore) {
                }
            }).start();
        }
        Thread.sleep(3000);
        startSignal.countDown();
        Thread.sleep(3000);
        finishSignal.await();
    }

    @Test
    public void testCollapse() throws Exception {
        String key = "testCollapse";
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch signal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(1);
        new Thread(() -> {
            try {
                startSignal.await();
                System.out.println("我是奔溃线程, 我要锁住它.");
                redisDistLock.lock(key, 5, TimeUnit.SECONDS);
                signal.countDown();
                Thread.sleep(1000);
                throwz("我奔溃了");
                System.out.println("我是奔溃线程, 我竟然没有奔溃，算了，不锁了.");
                redisDistLock.unlock(key);
            } catch (InterruptedException ignore) {
            }
        }, "奔溃").start();

        new Thread(() -> {
            try {
                startSignal.await();
                signal.await();
                System.out.println("我是正常线程, 我要获取锁.");
                redisDistLock.lock(key, 5, TimeUnit.SECONDS);
                System.out.println("我是正常线程, 我拿到锁了.");
                Thread.sleep(3000);
            } catch (InterruptedException ignore) {
            } finally {
                System.out.println("我是正常线程, 我要释放锁.");
                redisDistLock.unlock(key);
                finishSignal.countDown();
            }
        }).start();

        Thread.sleep(3000);
        startSignal.countDown();
        finishSignal.await();
    }

    private void throwz(String message) {
        throw new RuntimeException(message);
    }
}