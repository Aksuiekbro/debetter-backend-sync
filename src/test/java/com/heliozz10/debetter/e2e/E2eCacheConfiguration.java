package com.heliozz10.debetter.e2e;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
@EnableCaching
public class E2eCacheConfiguration {
    @Bean
    @Primary
    CacheManager e2eCacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
