package com.hnkjzyxy.ab.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-12 11:18
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RedisCache {
    String key() default "";
}
