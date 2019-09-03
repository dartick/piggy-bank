package org.xiaoheshan.piggy.bank.basis.concurrent;

import java.util.concurrent.Semaphore;

public class SemaphoreDeadLockTest {


    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(1);

        semaphore.acquire();

        semaphore.acquire();

        System.out.println("this is not a dead lock");
    }
}
