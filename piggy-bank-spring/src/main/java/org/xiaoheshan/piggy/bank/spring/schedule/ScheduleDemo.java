package org.xiaoheshan.piggy.bank.spring.schedule;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 任务调度Demo
 *
 * @author _Chf
 * @since 01-24-2018
 */
@Component
public class ScheduleDemo {

    @Scheduled(fixedRate = 1000L)
    public void scheduleTask() {
        System.out.println("I'm schedule task, date: " + new Date());
    }

}
