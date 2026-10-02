package com.hnkjzyxy.ab.config;

import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.UnknownHostException;

/**
 * Redis 缓存配置，注册缓存键生成器和对象序列化模板
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
public class RedisConfig {

    /**
     * 创建由类名、方法名和参数组成的缓存键生成器
     *
     * @return 缓存键生成器
     */
    @Bean
    public KeyGenerator keyGenerator() {
        return (o, method, params) -> {
            StringBuilder sb = new StringBuilder();
            /*sb.append(o.getClass().getName()); // 类目
            sb.append(method.getName()); // 方法名*/
            for (Object param : params) {
                sb.append(param.toString()); // 参数名
            }
            return DigestUtil.md5(sb.toString());
            //return sb.toString();
        };
    }

    /**
     * 配置字符串键及 JSON 对象值的 Redis 模板
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 支持 JSON 对象值的 Redis 模板
     * @throws UnknownHostException 主机地址解析失败时抛出
     */
    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory)
            throws UnknownHostException {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        //对value的json序列化配置
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        //String的序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        template.setValueSerializer(jackson2JsonRedisSerializer); // 对string的value进行jackson序列化
        template.setHashValueSerializer(jackson2JsonRedisSerializer);// 对hash的value进行jackson序列化
        return template;
    }

    /**
     * spingcache与redis结合的配置类
     *
     * @param cacheProperties Spring Boot 缓存配置
     * @return Redis 缓存序列化及有效期配置
     */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        // 设置缓存key的序列化方式
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        // 设置缓存value的序列化方式(JSON格式)
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }
}
