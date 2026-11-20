package com.shoplite.catalog;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Product lookups are cached in Redis. Spring Boot builds the RedisCacheManager
 * (TTL from spring.cache.redis.time-to-live); this customizer adjusts two things.
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CatalogCacheConfig {

    public static final String PRODUCTS_CACHE = "products";

    @Bean
    RedisCacheManagerBuilderCustomizer productsCacheCustomizer() {
        return builder -> {
            // Store products as readable JSON (try: redis-cli GET products::1) instead of Java serialization.
            RedisCacheConfiguration productsConfig = builder.cacheDefaults()
                    .serializeValuesWith(SerializationPair.fromSerializer(
                            new JacksonJsonRedisSerializer<>(ProductResponse.class)));
            builder.withCacheConfiguration(PRODUCTS_CACHE, productsConfig)
                    // Evict and put only after the database transaction commits.
                    // Otherwise a reader could cache the old value before the new one is visible.
                    .transactionAware();
        };
    }

}
