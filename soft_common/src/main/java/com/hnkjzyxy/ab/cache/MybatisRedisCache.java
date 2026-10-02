package com.hnkjzyxy.ab.cache;

import com.hnkjzyxy.ab.support.ApplicationContextHolder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ibatis.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 使用redis实现mybatis二级缓存
 */

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023-10-01 16:33
 */
public class MybatisRedisCache implements Cache {
    /**
     * 读写锁，保证缓存读写的线程安全
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    private final String id;
    private RedisTemplate redisTemplate;

    public MybatisRedisCache(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
    }

    private RedisTemplate getRedisTemplate() {
        //通过ApplicationContextHolder工具类获取RedisTemplate
        if (redisTemplate == null) {
            redisTemplate = (RedisTemplate) ApplicationContextHolder.getBeanByName("redisTemplate");
        }
        return redisTemplate;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void putObject(Object key, Object value) {
        //使用redis的Hash类型进行存储
        getRedisTemplate().opsForHash().put(id, DigestUtils.md5Hex(key.toString()), value);
    }

    @Override
    public Object getObject(Object key) {
        try {
            //根据key从redis中获取数据
            return getRedisTemplate().opsForHash().get(id, DigestUtils.md5Hex(key.toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object removeObject(Object key) {
        if (key != null) {
            getRedisTemplate().delete(key.toString());
        }
        return null;
    }

    @Override
    public void clear() {
        getRedisTemplate().delete(id.toString());
    }

    @Override
    public int getSize() {
        return getRedisTemplate().opsForHash().size(id).intValue();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }

}