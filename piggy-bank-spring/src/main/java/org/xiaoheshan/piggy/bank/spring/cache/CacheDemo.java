package org.xiaoheshan.piggy.bank.spring.cache;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author _Chf
 * @since 01-24-2018
 */
@Component
public class CacheDemo {

    /**
     * 每次使用{@link org.springframework.cache.annotation.CachePut}, 会执行方法并对结果缓存, 可用来执行刷新缓存操作
     * @param name
     * @return
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_MANAGER_NAME, key = "#name")
    public String getName(String name) {
        return getNameInternal(name);
    }

    private String getNameInternal(String name) {
        System.out.println("I'm input internal, I just only occurred once");
        return name;
    }

}
