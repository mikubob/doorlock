package com.hnkjzyxy.ab.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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
     * 物理文件位置：{@code soft_common/src/main/resources/lua/release_lock.lua}。
     * 与锁工具一起打包，独立模块测试及复用时也能加载。
     * </p>
     * <p>
     * 使用 classpath 路径查找，不依赖宿主应用的文件系统路径。
     * </p>
     */
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = loadScript("lua/release_lock.lua");

    /**
     * 比对持有者令牌后续期的 Lua 脚本，避免延长其他实例持有的锁。
     */
    private static final RedisScript<Long> RENEW_LOCK_SCRIPT = RedisScript.of(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('EXPIRE', KEYS[1], ARGV[2]) end return 0", Long.class);

    /**
     * 字符串 Redis 数据操作模板
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化RedisLockUtils
     *
     * @param stringRedisTemplate 字符串 Redis 数据操作模板
     */
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
        if (ttlSeconds <= 0 || ttlSeconds > Long.MAX_VALUE / 1000) {
            throw new IllegalArgumentException("锁有效期必须为正数且可转换为毫秒");
        }
        String token = UUID.randomUUID().toString();
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success) ? token : null;
    }

    /**
     * 获取自动续期的分布式锁
     * <p>
     * Redis 故障直接向上抛出，不降级为进程内锁。
     * </p>
     *
     * @param key        锁名称
     * @param ttlSeconds 租约有效期，单位为秒
     * @return 锁租约，未获取到锁时返回 null
     * @throws IllegalArgumentException 租约有效期无效
     */
    public LockLease tryLease(String key, long ttlSeconds) {
        String token = tryLock(key, ttlSeconds);
        if (token == null) {
            return null;
        }
        try {
            return new LockLease(key, token, ttlSeconds);
        } catch (RuntimeException e) {
            unlock(key, token);
            throw e;
        }
    }

    /**
     * 原子校验持有者并续期
     *
     * @param key        锁名称
     * @param token      持有者令牌
     * @param ttlSeconds 续期后的有效期，单位为秒
     * @return 当前实例仍持有锁且续期成功时返回 true
     */
    public boolean renew(String key, String token, long ttlSeconds) {
        Long result = stringRedisTemplate.execute(RENEW_LOCK_SCRIPT, Collections.singletonList(key),
                token, String.valueOf(ttlSeconds));
        return Long.valueOf(1).equals(result);
    }

    /**
     * 分布式锁租约
     * <p>
     * 定期校验令牌并续期，关闭时停止续期并安全释放锁。
     * </p>
     */
    public class LockLease implements AutoCloseable {
        /**
         * 锁名称
         */
        private final String key;
        /**
         * 当前持有者令牌
         */
        private final String token;
        /**
         * 租约有效期，单位为秒
         */
        private final long ttlSeconds;
        /**
         * 租约续期线程池
         */
        private final ScheduledExecutorService scheduler;
        /**
         * 周期续期任务
         */
        private final ScheduledFuture<?> renewal;
        /**
         * 租约是否已失效，失效后不再恢复
         */
        private boolean lost;
        /**
         * 租约是否已关闭
         */
        private boolean closed;

        /**
         * 创建租约并启动周期续期任务
         *
         * @param key        锁名称
         * @param token      当前持有者令牌
         * @param ttlSeconds 租约有效期，单位为秒
         */
        private LockLease(String key, String token, long ttlSeconds) {
            this.key = key;
            this.token = token;
            this.ttlSeconds = ttlSeconds;
            scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "schedule-lock-renewal");
                thread.setDaemon(true);
                return thread;
            });
            long intervalMillis = Math.max(100, ttlSeconds * 1000 / 3);
            renewal = scheduler.scheduleWithFixedDelay(this::renewInBackground,
                    intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * 校验当前实例仍持有锁并主动续期
         * <p>
         * 写入前和提交前调用；续期失败后当前租约永久失效。
         * </p>
         *
         * @throws IllegalStateException 租约已关闭、已失效或令牌不匹配
         */
        public synchronized void requireOwned() {
            if (closed || lost) {
                throw new IllegalStateException("课表同步锁已失效，本次同步放弃");
            }
            try {
                if (!renew(key, token, ttlSeconds)) {
                    lost = true;
                    throw new IllegalStateException("课表同步锁已不属于当前实例，本次同步放弃");
                }
            } catch (RuntimeException e) {
                lost = true;
                throw e;
            }
        }

        /**
         * 执行周期续期，失败后停止调度并阻止后续写入。
         */
        private void renewInBackground() {
            try {
                requireOwned();
            } catch (RuntimeException e) {
                log.error("同步锁续期失败，后续写入将被阻止：key={}", key, e);
                scheduler.shutdown();
            }
        }

        /**
         * 停止续期并释放当前持有者的锁，重复关闭不会重复释放。
         */
        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            renewal.cancel(false);
            scheduler.shutdownNow();
            unlock(key, token);
        }
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
