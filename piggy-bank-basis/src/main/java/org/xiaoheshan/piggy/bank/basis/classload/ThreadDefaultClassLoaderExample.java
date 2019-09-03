package org.xiaoheshan.piggy.bank.basis.classload;

/**
 * @author _Chf
 * @since 08-21-2019
 */
public class ThreadDefaultClassLoaderExample {


    public static void main(String[] args) throws InterruptedException {
        Thread thread0 = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Thread.currentThread().setContextClassLoader(new MyClassLoader());
                    }
                }
        );
        thread0.start();
        thread0.join();
        Thread thread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                        System.out.println(contextClassLoader);
                    }
                }
        );
        thread.start();
        thread.join();
    }

    public static class MyClassLoader extends ClassLoader {}
}
