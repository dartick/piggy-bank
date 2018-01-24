package org.xiaoheshan.piggy.bank.spring.schedule;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xiaoheshan.piggy.bank.spring.BaseTest;

/**
 * @author _Chf
 * @since 01-24-2018
 */
public class ScheduleDemoTest extends BaseTest {

    @Autowired
    private ScheduleDemo scheduleDemo;

    @Test
    public void scheduleTask() throws InterruptedException {
        Thread.sleep(10000);
    }
}