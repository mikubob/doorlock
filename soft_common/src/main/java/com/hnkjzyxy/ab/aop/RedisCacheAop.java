package com.hnkjzyxy.ab.aop;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.hnkjzyxy.ab.annotation.RedisCache;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务缓存切面，根据方法及参数生成缓存键并缓存返回数据
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-12 11:22
 */
@Component
@Aspect
public class RedisCacheAop {

    /**
     * 业务缓存切面，根据方法及参数生成缓存键并缓存返回数据
     */
    @Aspect
    @Component
    public class CacheAspect {

        /**
         * 缓存键拼接使用的空字符串
         */
        private static final String EMPTY = "";
        /**
         * 缓存键中的分隔符
         */
        private static final String POINT = "::";
        /**
         * 统一缓存键前缀
         */
        private static final String CACHE_KEY_PREFIX = "cache.aspect:";

        /**
         * Redis 数据操作工具
         */
        @Resource
        private RedisUtils redisUtils;

        /**
         * 匹配所有使用以下注解的方法
         * 注意：缓存是基于类+方法+参数内容做的缓存key,重载方法可能会出现问题
         * 禁止在同一个类，方法名相同的两个方法使用
         *
         * @see RedisCache
         */
        @Pointcut("@annotation(com.hnkjzyxy.ab.annotation.RedisCache)")
        public void pointCut() {
        }

        /**
         * 拦截添加缓存注解的方法
         *
         * @param pjpParam 被拦截方法的参数数组
         * @param redisCache 业务缓存注解配置
         * @return 缓存命中值或被拦截方法执行后得到的结果
         * @throws Throwable 被拦截的业务方法执行失败时抛出
         * @see RedisCache
         */
        @Around("pointCut()&&@annotation(redisCache)")
        public Object beforeAround(ProceedingJoinPoint pjpParam, RedisCache redisCache) throws Throwable {
            //注解为空
            if (null == redisCache) {
                return pjpParam.proceed(pjpParam.getArgs());
            }
            //仅用于方法
            if (null == pjpParam.getSignature() || !(pjpParam.getSignature() instanceof MethodSignature)) {
                return pjpParam.proceed(pjpParam.getArgs());
            }
            //方法实例
            Method method = ((MethodSignature) pjpParam.getSignature()).getMethod();
            //方法类名
            String className = pjpParam.getSignature().getDeclaringTypeName();
            //方法名
            String methodName = method.getName();

            //从redis读
            String key = getCacheKey(redisCache, className, methodName, pjpParam);
            String proceed = redisUtils.get(key);
            if (ObjectUtil.isNotEmpty(proceed)) {
                return JSON.parseObject(proceed, method.getReturnType());
            } else {
                redisUtils.set(key, JSON.toJSONString(pjpParam.proceed()));
                return pjpParam.proceed();
            }
        }

        /**
         * 生成缓存key
         *
         * @param redisCache 业务缓存注解配置
         * @param className 被拦截方法所属类名
         * @param methodName 被拦截方法名称
         * @param pjpParam 被拦截方法的参数数组
         * @return 由缓存前缀、类名、方法名和参数组成的缓存键
         */
        private String getCacheKey(RedisCache redisCache, String className, String methodName, ProceedingJoinPoint pjpParam) {
            //缓存key前缀
            String keyPrefix = redisCache.key();
            if (EMPTY.equals(keyPrefix)) {
                keyPrefix = methodName;
            }
            //方法全路径（类名+方法名）
            String methodPath = className + POINT + methodName;
            //若方法参数为空
            if (pjpParam.getArgs() == null || pjpParam.getArgs().length == 0) {
                return keyPrefix + POINT + DigestUtils.md5Hex(methodPath);
            }
            //参数序号
            int i = 0;
            //按照参数顺序,拼接方法参数
            Map<String, Object> paramMap = new LinkedHashMap<>(pjpParam.getArgs().length);
            for (Object obj : pjpParam.getArgs()) {
                i++;
                if (obj != null) {
                    paramMap.put(obj.getClass().getName() + i, obj);
                } else {
                    paramMap.put("NULL" + i, "NULL");
                }
            }
            String paramJson = JSON.toJSONString(paramMap);
            return keyPrefix + POINT + DigestUtils.md5Hex(paramJson);
        }

    }


}
