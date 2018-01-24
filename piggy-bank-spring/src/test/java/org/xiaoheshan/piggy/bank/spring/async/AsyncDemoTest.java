package org.xiaoheshan.piggy.bank.spring.async;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xiaoheshan.piggy.bank.spring.BaseTest;

import java.util.concurrent.Future;

/**
 * @author _Chf
 * @since 01-24-2018
 */
public class AsyncDemoTest extends BaseTest {

    @Autowired
    private AsyncDemo asyncDemo;

    @Test
    public void startTaskWithNoResult() throws Exception {
        asyncDemo.startTaskWithNoResult();
        Thread.sleep(3000);
    }

    @Test
    public void startTaskWithResult() throws Exception {
        Future<Integer> future = asyncDemo.startTaskWithResult(100);
        Thread.sleep(1000);
        System.out.println("future result : " + future.get());
        Thread.sleep(2000);
    }

    @Test
    public void startTaskWithException() throws Exception {
        asyncDemo.startTaskWithException();
        Thread.sleep(3000);
    }
}