package cn.edu.nju.sicp.configs;

import cn.edu.nju.sicp.models.Statistics;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String SCORE_CACHE_NAME = "score";
    public static final RedisCacheConfiguration SCORE_CACHE_CONFIG;

    static {
        SCORE_CACHE_CONFIG = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ZERO)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new Jackson2JsonRedisSerializer<>(Statistics.class)));
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                .initialCacheNames(Set.of(SCORE_CACHE_NAME))
                .withInitialCacheConfigurations(Map.of(SCORE_CACHE_NAME, SCORE_CACHE_CONFIG))
                .build();
    }

}
