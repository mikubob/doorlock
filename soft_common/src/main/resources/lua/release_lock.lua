--[[
  Redis 分布式锁 —— 安全释放脚本
  （classpath:lua/release_lock.lua）

  作用：
    仅当锁的持有者令牌与传入令牌一致时才删除锁，避免下面这种「误删他人锁」：
      A 获取锁 → A 的业务执行超时，锁被 TTL 回收 → B 获取到同一把锁
      → A 执行完毕回来释放锁，把 B 的锁删掉了 → 第三个实例也能拿到锁，
        分布式锁退化为无锁，破坏互斥性。

  参数：
    KEYS[1]  锁 key
    ARGV[1]  持有者令牌（UUID）

  返回：
    1  释放成功
    0  锁已不属于当前持有者（已被 TTL 回收或被他人重新获取），不做任何操作

  注意（重要）：
    必须配合 StringRedisTemplate 使用。项目自定义的 RedisTemplate 使用 Jackson JSON
    序列化，写入的值会带上引号，与本脚本 GET 出来的值不相等 ——
    结果是锁永远走不到 DEL 分支，只能等 TTL 过期。
--]]

local lockKey = KEYS[1]
local token   = ARGV[1]

if redis.call('GET', lockKey) == token then
    return redis.call('DEL', lockKey)
end

return 0
