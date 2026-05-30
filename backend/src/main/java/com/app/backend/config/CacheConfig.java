package com.app.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory {@link org.springframework.cache.concurrent.ConcurrentMapCacheManager} for
 * low-traffic or single-node deployments. For multi-instance production, switch to a shared
 * provider (Redis, Caffeine, etc.) without changing call sites.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("phasesById", "khayabansById");
    }
}
