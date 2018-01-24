package org.xiaoheshan.piggy.bank.spring.cache;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xiaoheshan.piggy.bank.spring.BaseTest;

import static org.junit.Assert.*;

/**
 * @author _Chf
 * @since 01-24-2018
 */
public class CacheDemoTest extends BaseTest {

    @Autowired
    private CacheDemo cacheDemo;

    @Test
    public void input() {
        cacheDemo.getName("test");
        cacheDemo.getName("test");
        cacheDemo.getName("test");
        cacheDemo.getName("test");
    }
}