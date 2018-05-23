package org.xiaoheshan.piggy.bank.dubbo.provider;

import com.alibaba.dubbo.config.annotation.Service;
import org.xiaoheshan.piggy.bank.dubbo.api.DemoService;

/**
 * @author _Chf
 * @since 05-23-2018
 */
@Service
public class Provider implements DemoService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " (from Spring Boot)";
    }
}
