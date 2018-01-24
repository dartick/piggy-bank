package org.xiaoheshan.piggy.bank.spring;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author _Chf
 * @since 01-24-2018
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BaseTest {

    @Before
    public void beforeTest() {
        System.out.println("------------------Test Start------------------");
    }

    @After
    public void afterTest() {
        System.out.println("------------------Test End--------------------");
    }

}
