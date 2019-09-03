package org.xiaoheshan.piggy.bank.basis.concurrent;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CyclicBarrierImplByCountDownLatchTest {

    private int testThreadCount = 16;

    private void run() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(testThreadCount);

        for (int i = 0; i < testThreadCount; i++) {
            new Thread(() -> {
                countDownLatch.countDown();
                System.out.println(Thread.currentThread().getName() + ", count down: " + countDownLatch.getCount());
                try {
                    countDownLatch.await();
                    System.out.println(Thread.currentThread().getName() + ", wake up !!!");
                } catch (Exception ignore) {
                }
            }).start();
        }

        TimeUnit.SECONDS.sleep(2);
    }


    public static void main(String[] args) throws Exception {

        new CyclicBarrierImplByCountDownLatchTest().run();
    }

}
