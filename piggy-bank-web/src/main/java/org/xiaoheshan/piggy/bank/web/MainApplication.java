package org.xiaoheshan.piggy.bank.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动器
 *
 * @author _Chf
 * @since 01-23-2018
 */
@SpringBootApplication
@MapperScan("org.xiaoheshan.piggy.bank.dal")
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}
