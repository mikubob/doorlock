package com.hnkjzyxy.ab.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式锁工具
 * <p>
 * 依赖 {@link StringRedisTemplate}（字符串序列化）而非项目自定义的
 * {@code RedisTemplate}（Jackson JSON 序列化）：后者写入的值会带上引号，
 * 与 Lua 脚本中比对的值不一致，会导致锁永远无法正确释放。
 * </p>
 *
 * @version 1.1
 * @date 2026-09-29
 */
@Slf4j
@Component
public class RedisLockUtils {

    /**
     * 释放锁的 Lua 脚本，独立存放于 classpath:lua/release_lock.lua
     * <p>
     * 物理文件位置：{@code soft_main/src/main/resources/lua/release_lock.lua}。<br>
     * 本类位于 soft_common，但按项目约定「资源统一收敛到 soft_main」，
     * 脚本文件同样放在 soft_main（与 {@code mapper/*.xml} 的做法一致：
     * 接口在 soft_mapper、XML 在 soft_main）。
     * </p>
     * <p>
     * 这里用 classpath 路径而非文件系统路径查找：编译后各模块的 resources 会被
     * 合并到同一个运行时 classpath（fat jar 内为 {@code BOOT-INF/classes/lua/}），
     * 因此 soft_common 的代码读取 soft_main 下的资源没有问题。
     * </p>
     */
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = loadScript("lua/release_lock.lua");

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLockUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 从 classpath 加载 Lua 脚本
     * <p>
     * 使用 {@link RedisScript#of(org.springframework.core.io.Resource, Class)}：
     * Spring Data Redis 会缓存脚本的 SHA1 并以 EVALSHA 执行，避免每次调用都传输脚本文本。
     * </p>
     *
     * @param location 脚本在 classpath 下的路径
     * @return Redis 脚本对象
     */
    private static RedisScript<Long> loadScript(String location) {
        ClassPathResource resource = new ClassPathResource(location);
        if (!resource.exists()) {
            // 本类是 @Component，Spring 启动时就会实例化它并触发本类静态初始化，
            // 因此「资源漏打包」会在启动阶段直接暴露，而不是等到凌晨 3 点第一次释放锁
            throw new IllegalStateException("Redis Lua 脚本不存在，请检查资源是否被打包：" + location);
        }
        return RedisScript.of(resource, Long.class);
    }

    /**
     * 尝试获取锁（抢不到立即返回，不等待）
     *
     * @param key        锁 key
     * @param ttlSeconds 锁自动过期时间（秒）
     * @return 抢锁成功返回锁令牌（释放时需原样传回），失败返回 null
     */
    public String tryLock(String key, long ttlSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * 释放锁（执行 lua/release_lock.lua，比对令牌后删除，保证只释放自己持有的锁）
     *
     * @param key   锁 key
     * @param token 获取锁时返回的令牌
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception e) {
            log.warn("释放 Redis 分布式锁失败，锁将由 TTL 自动过期：key={}", key, e);
        }
    }
}
