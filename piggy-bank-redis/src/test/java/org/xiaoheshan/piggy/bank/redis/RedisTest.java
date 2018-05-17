package org.xiaoheshan.piggy.bank.redis;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.support.atomic.RedisAtomicInteger;

import java.util.concurrent.CountDownLatch;

/**
 * @author _Chf
 * @since 05-15-2018
 */
public class RedisTest extends BaseTest {

    private static final String KEY = "testConcurrentIncrement";
    private RedisAtomicInteger atomicInteger;

    @Autowired
    private JedisConnectionFactory connectionFactory;

    @Before
    public void setup() {
        atomicInteger = new RedisAtomicInteger(KEY, connectionFactory);
    }

    @Test
    public void testConcurrentIncrement() throws InterruptedException {
        int threadCount = 128;
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch finishSignal = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    startSignal.await();
                    int result = atomicInteger.incrementAndGet();
                    System.out.println("我是线程: " + finalI + ", 完成自增: " + result);
                    finishSignal.countDown();
                } catch (InterruptedException ignore) {
                }
            }).start();
        }

        startSignal.countDown();
        finishSignal.await();
        int result = atomicInteger.get();
        System.out.println("线程数为: " + threadCount + ", 结果为: " + result);
    }
}
