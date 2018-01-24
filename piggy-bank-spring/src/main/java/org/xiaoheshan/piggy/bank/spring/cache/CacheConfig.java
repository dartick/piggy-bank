package org.xiaoheshan.piggy.bank.spring.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置
 * 配置{@link CacheManager}可整合其他缓存框架
 * 可结合{@link org.springframework.scheduling.annotation.Scheduled}
 * {@link org.springframework.cache.annotation.CacheEvict}
 * 设置缓存时间
 * 可整合Ehcache来提高缓存能力
 *
 * @author _Chf
 * @since 01-24-2018
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_MANAGER_NAME = "default-cache-manager";

    @Bean(CACHE_MANAGER_NAME)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
