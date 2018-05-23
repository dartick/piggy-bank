package org.xiaoheshan.piggy.bank.dubbo.consumer;

import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xiaoheshan.piggy.bank.dubbo.api.DemoService;

/**
 * @author _Chf
 * @since 05-23-2018
 */
@RestController
public class Consumer {

    @Reference
    private DemoService demoService;

    @GetMapping("/consume")
    public String consume() {
        return demoService.sayHello("test");
    }
}
