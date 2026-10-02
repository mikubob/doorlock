package com.hnkjzyxy.ab.interceptor.impl;

import com.alibaba.fastjson.JSON;
import com.hnkjzyxy.ab.annotation.RepeatSubmit;
import com.hnkjzyxy.ab.interceptor.RepeatSubmitInterceptor;
import com.hnkjzyxy.ab.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 判断请求url和数据是否和上一次相同，
 * 如果和上次相同，则是重复提交表单。 有效时间为10秒内。
 */
@Component
public class SameUrlDataInterceptor extends RepeatSubmitInterceptor {
    /**
     * 重复提交记录的 Redis 键前缀
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";
    /**
     * 重复提交记录中的请求参数字段名
     */
    public final String REPEAT_PARAMS = "repeatParams";
    /**
     * 重复提交记录中的提交时间字段名
     */
    public final String REPEAT_TIME = "repeatTime";

    /**
     * 请求中的认证令牌头名称
     */
    @Value("${absolute.jwt.header}")
    private String header;

    /**
     * Redis 数据操作工具
     */
    @Autowired
    private RedisUtils redisCache;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation) {
        String nowParams = JSON.toJSONString(request.getParameterMap());
        Map<String, Object> nowDataMap = new HashMap<String, Object>();
        nowDataMap.put(REPEAT_PARAMS, nowParams);
        nowDataMap.put(REPEAT_TIME, System.currentTimeMillis());

        // 请求地址（作为存放cache的key值）
        String url = request.getRequestURI();

        String head = request.getHeader(header);

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = REPEAT_SUBMIT_KEY + url + head;

        Object sessionObj = redisCache.getCacheObject(cacheRepeatKey);
        if (sessionObj != null) {
            Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
            if (sessionMap.containsKey(url)) {
                Map<String, Object> preDataMap = (Map<String, Object>) sessionMap.get(url);
                if (compareParams(nowDataMap, preDataMap) && compareTime(nowDataMap, preDataMap, annotation.interval())) {
                    return true;
                }
            }
        }
        Map<String, Object> cacheMap = new HashMap<String, Object>();
        cacheMap.put(url, nowDataMap);
        redisCache.setCacheObject(cacheRepeatKey, cacheMap, annotation.interval(), TimeUnit.MILLISECONDS);
        return false;
    }

    /**
     * 判断参数是否相同
     *
     * @param nowMap 当前提交的请求参数及时间信息
     * @param preMap 上一次提交的请求参数及时间信息
     * @return 操作或条件校验结果
     */
    private boolean compareParams(Map<String, Object> nowMap, Map<String, Object> preMap) {
        String nowParams = (String) nowMap.get(REPEAT_PARAMS);
        String preParams = (String) preMap.get(REPEAT_PARAMS);
        return nowParams.equals(preParams);
    }

    /**
     * 判断两次间隔时间
     *
     * @param nowMap 当前提交的请求参数及时间信息
     * @param preMap 上一次提交的请求参数及时间信息
     * @param interval 允许连续提交的最小间隔，单位为毫秒
     * @return 操作或条件校验结果
     */
    private boolean compareTime(Map<String, Object> nowMap, Map<String, Object> preMap, int interval) {
        long time1 = (Long) nowMap.get(REPEAT_TIME);
        long time2 = (Long) preMap.get(REPEAT_TIME);
        if ((time1 - time2) < interval) {
            return true;
        }
        return false;
    }
}
