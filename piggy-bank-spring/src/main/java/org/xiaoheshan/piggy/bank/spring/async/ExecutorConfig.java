package org.xiaoheshan.piggy.bank.spring.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * 自定义异常处理器需要实现{@link AsyncConfigurer},
 * 无特殊需求建议使用spring默认的异常处理器
 * {@link org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler}
 *
 * @author _Chf
 * @since 01-24-2018
 */
@Configuration
@EnableAsync
public class ExecutorConfig implements AsyncConfigurer {

    public static final String TASK_EXECUTOR_NAME = "default-async-task-executor";

    @Override
    public Executor getAsyncExecutor() {
        return this.taskExecutor();
    }

    @Bean(TASK_EXECUTOR_NAME)
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(10);
        executor.setRejectedExecutionHandler(new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }
        });
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new MyAsyncUncaughtExceptionHandler();
    }

    private static class MyAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
        private final Logger logger = LoggerFactory.getLogger(MyAsyncUncaughtExceptionHandler.class);
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            logger.error("{}线程发生异常, params={}, exception={}", Thread.currentThread().getName(), Arrays.toString(params), ex);
        }
    }

}
