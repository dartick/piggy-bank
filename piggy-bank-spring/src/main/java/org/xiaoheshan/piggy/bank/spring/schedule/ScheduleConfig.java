package org.xiaoheshan.piggy.bank.spring.schedule;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 任务调度配置
 * 使用{@link EnableScheduling}使{@link org.springframework.scheduling.annotation.Scheduled}生效
 * 线程池等细粒度配置的实现{@link SchedulingConfigurer}
 *
 * @author _Chf
 * @since 01-24-2018
 */
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer{

    /**
     * 配置任务调度管理
     * @param taskRegistrar 默认的任务调度
     */
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    }

}
