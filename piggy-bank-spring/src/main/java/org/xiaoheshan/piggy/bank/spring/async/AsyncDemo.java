package org.xiaoheshan.piggy.bank.spring.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.Future;

/**
 * 异步Demo, {@link Async#value()}可指定线程池, 建议填写, 以表明使用的线程池
 *
 * @author _Chf
 * @since 01-24-2018
 */
@Component
@Async(ExecutorConfig.TASK_EXECUTOR_NAME)
public class AsyncDemo {

    public void startTaskWithNoResult() {
        System.out.println("I'm async task with no result, thread: " + Thread.currentThread().getName());
    }

    /**
     * 返回的实例必须是{@link AsyncResult}
     * @param input
     * @return
     */
    public Future<Integer> startTaskWithResult(Integer input) {
        System.out.println("I'm async task with result, input: " + input + ", thread: " + Thread.currentThread().getName());
        Future<Integer> future = new AsyncResult<Integer>(input + 1);
        return future;
    }

    public void startTaskWithException() {
        System.out.println("I'm async task with exception, thread: " + Thread.currentThread().getName());
        throw new RuntimeException("I'm async task with exception");
    }

}
