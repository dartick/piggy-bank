package org.xiaoheshan.piggy.bank.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello 控制器
 *
 * @author _Chf
 * @since 01-23-2018
 */
@RestController
public class HelloController {

    @RequestMapping("/")
    public String hello() {
        return "Hello World !!!";
    }

}
