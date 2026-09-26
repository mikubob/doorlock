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
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-12 11:22
 */
@Component
@Aspect
public class RedisCacheAop {

    /**
     * 统一缓存自定义注解拦截实现
     */
    @Aspect
    @Component
    public class CacheAspect {

        private static final String EMPTY = "";
        private static final String POINT = "::";
        private static final String CACHE_KEY_PREFIX = "cache.aspect:";

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
         * @param pjpParam
         * @return
         * @throws Throwable
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
         * @param redisCache
         * @param className
         * @param methodName
         * @param pjpParam
         * @return
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
