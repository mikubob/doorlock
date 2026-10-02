package com.hnkjzyxy.ab.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务 Redis 缓存注解，指定缓存键前缀
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-12 11:18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCache {
    /**
     * 获取业务缓存键前缀
     *
     * @return 业务缓存键前缀
     */
    String key() default "";
}
