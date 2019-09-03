package org.xiaoheshan.piggy.bank.basis.jmm;

/**
 * @author _Chf
 * @since 08-01-2019
 */
public class UnexpectedExample {

    public int x = 0;
    public int y = 0;
    public int r1 = 0;
    public int r2 = 0;

    public void fun1() {
        x = 1; // 1
        r1 = y; // 2
    }

    public void fun2() {
        y = 1; // 3
        r2 = x;  // 4
    }
    /**
     * 1. x = 1 , r1 = 0, y = 1, r2 = 1
     * 2. x = 1, y = 1, r1 = 1, r2 = 1
     * 3. y = 1, r2 = 0, x = 1, r1 = 1
     *
     * r1= 0 , r2 = 0
     */

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 20000; i++) {
            UnexpectedExample unexpectedExample = new UnexpectedExample();
            Thread t1 = new Thread(() -> {
                unexpectedExample.fun1();
            });
            Thread t2 = new Thread(() -> {
                unexpectedExample.fun2();
            });
            t1.start();
            t2.start();
            t1.join();
            t2.join();
            if (unexpectedExample.r1 == 0 && unexpectedExample.r2 == 0) {
                throw new AssertionError();
            }
        }
    }
}
