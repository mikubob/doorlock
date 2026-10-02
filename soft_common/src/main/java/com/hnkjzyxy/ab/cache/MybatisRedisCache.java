package com.hnkjzyxy.ab.cache;

import com.hnkjzyxy.ab.support.ApplicationContextHolder;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.ibatis.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于 Redis 的 MyBatis 二级缓存实现
 *
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
    /**
     * 缓存所属的 MyBatis 命名空间标识
     */
    private final String id;
    /**
     * Redis 数据操作模板
     */
    private RedisTemplate redisTemplate;

    /**
     * 初始化指定命名空间的 MyBatis 二级缓存
     *
     * @param id 缓存命名空间标识，不允许为 null
     * @throws IllegalArgumentException 命名空间标识为 null 时抛出
     */
    public MybatisRedisCache(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
    }

    /**
     * 延迟获取 Spring 容器中的 Redis 模板
     *
     * @return 用于二级缓存的 Redis 模板
     */
    private RedisTemplate getRedisTemplate() {
        //通过ApplicationContextHolder工具类获取RedisTemplate
        if (redisTemplate == null) {
            redisTemplate = (RedisTemplate) ApplicationContextHolder.getBeanByName("redisTemplate");
        }
        return redisTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putObject(Object key, Object value) {
        //使用redis的Hash类型进行存储
        getRedisTemplate().opsForHash().put(id, DigestUtils.md5Hex(key.toString()), value);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object removeObject(Object key) {
        if (key != null) {
            getRedisTemplate().delete(key.toString());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        getRedisTemplate().delete(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSize() {
        return getRedisTemplate().opsForHash().size(id).intValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }

}
