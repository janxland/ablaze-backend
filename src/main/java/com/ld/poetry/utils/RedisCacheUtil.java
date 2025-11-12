package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存工具类
 * 
 * 实现缓存雪崩、缓存穿透、缓存击穿的防护机制：
 * 
 * 1. 缓存雪崩（Cache Avalanche）
 *    - 问题：大量缓存同时过期，导致大量请求直接访问数据库
 *    - 解决方案：
 *      a) 设置随机过期时间，避免同时过期
 *      b) 使用多级缓存（本地缓存 + Redis）
 *      c) 缓存预热，提前加载热点数据
 * 
 * 2. 缓存穿透（Cache Penetration）
 *    - 问题：查询不存在的数据，绕过缓存直接访问数据库
 *    - 解决方案：
 *      a) 布隆过滤器：快速判断数据是否存在
 *      b) 缓存空对象：将空结果也缓存，设置较短过期时间
 *      c) 参数校验：对请求参数进行校验，过滤无效请求
 * 
 * 3. 缓存击穿（Cache Breakdown）
 *    - 问题：热点数据过期，大量并发请求同时访问数据库
 *    - 解决方案：
 *      a) 互斥锁：使用分布式锁，只允许一个线程查询数据库
 *      b) 永不过期：热点数据设置永不过期，异步更新
 *      c) 逻辑过期：设置逻辑过期时间，后台异步刷新
 */
@Component
@Slf4j
public class RedisCacheUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private BloomFilterUtil bloomFilterUtil;

    /**
     * 空对象标识
     */
    private static final String NULL_VALUE = "NULL";

    /**
     * 空对象过期时间（秒）- 较短，防止缓存大量无效数据
     */
    private static final long NULL_VALUE_EXPIRE = 60;

    /**
     * 默认过期时间（秒）
     */
    private static final long DEFAULT_EXPIRE = 3600;

    /**
     * 随机过期时间范围（秒）- 用于防止缓存雪崩
     */
    private static final long RANDOM_EXPIRE_RANGE = 300;

    /**
     * 获取随机过期时间，防止缓存雪崩
     */
    private long getRandomExpire(long baseExpire) {
        long random = (long) (Math.random() * RANDOM_EXPIRE_RANGE);
        return baseExpire + random;
    }

    /**
     * 设置缓存（带随机过期时间，防止缓存雪崩）
     */
    public void set(String key, Object value) {
        set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 设置缓存（带随机过期时间）
     */
    public void set(String key, Object value, long expireSeconds) {
        if (value == null) {
            // 缓存空对象，防止缓存穿透
            redisTemplate.opsForValue().set(key, NULL_VALUE, NULL_VALUE_EXPIRE, TimeUnit.SECONDS);
        } else {
            // 使用随机过期时间，防止缓存雪崩
            long randomExpire = getRandomExpire(expireSeconds);
            redisTemplate.opsForValue().set(key, value, randomExpire, TimeUnit.SECONDS);
        }
    }

    /**
     * 获取缓存（防止缓存穿透和击穿）
     */
    public Object get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (NULL_VALUE.equals(value)) {
            // 返回 null，表示数据不存在（已缓存空对象）
            return null;
        }
        return value;
    }

    /**
     * 获取缓存，如果不存在则查询数据库（防止缓存穿透、击穿、雪崩）
     * 
     * @param key 缓存key
     * @param moduleName 业务模块名称（用于布隆过滤器）
     * @param id 数据ID（用于布隆过滤器）
     * @param dataLoader 数据加载器（查询数据库的函数）
     * @param expireSeconds 过期时间（秒）
     * @return 数据对象
     */
    public <T> T get(String key, String moduleName, String id, 
                     DataLoader<T> dataLoader, long expireSeconds) {
        // 1. 先查询缓存
        Object value = get(key);
        if (value != null) {
            // 如果是空对象标识，返回 null
            if (NULL_VALUE.equals(value)) {
                return null;
            }
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        }

        // 2. 使用布隆过滤器判断数据是否存在（防止缓存穿透）
        if (StringUtils.hasText(moduleName) && StringUtils.hasText(id)) {
            if (!bloomFilterUtil.mightContain(moduleName, id)) {
                // 布隆过滤器判断不存在，直接返回 null，避免查询数据库
                log.debug("布隆过滤器判断数据不存在: module={}, id={}", moduleName, id);
                // 缓存空对象，防止重复查询
                set(key, null, NULL_VALUE_EXPIRE);
                return null;
            }
        }

        // 3. 使用分布式锁防止缓存击穿
        String lockKey = "lock:" + key;
        try {
            // 尝试获取分布式锁（使用 Redis SETNX 实现）
            if (tryLock(lockKey, 10, TimeUnit.SECONDS)) {
                try {
                    // 双重检查，再次查询缓存
                    value = get(key);
                    if (value != null) {
                        if (NULL_VALUE.equals(value)) {
                            return null;
                        }
                        @SuppressWarnings("unchecked")
                        T result = (T) value;
                        return result;
                    }

                    // 4. 查询数据库
                    T data = dataLoader.load();
                    
                    if (data != null) {
                        // 5. 将数据写入缓存
                        set(key, data, expireSeconds);
                        // 6. 将数据ID添加到布隆过滤器
                        if (StringUtils.hasText(moduleName) && StringUtils.hasText(id)) {
                            bloomFilterUtil.add(moduleName, id);
                        }
                        return data;
                    } else {
                        // 数据不存在，缓存空对象（防止缓存穿透）
                        set(key, null, NULL_VALUE_EXPIRE);
                        return null;
                    }
                } finally {
                    // 释放锁
                    releaseLock(lockKey);
                }
            } else {
                // 获取锁失败，等待一段时间后重试
                Thread.sleep(50);
                return get(key, moduleName, id, dataLoader, expireSeconds);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取分布式锁被中断", e);
            return null;
        } catch (Exception e) {
            log.error("获取缓存数据异常: key={}", key, e);
            return null;
        }
    }

    /**
     * 数据加载器接口
     */
    @FunctionalInterface
    public interface DataLoader<T> {
        T load();
    }

    /**
     * 尝试获取分布式锁（使用 Redis SETNX 实现）
     */
    private boolean tryLock(String lockKey, long timeout, TimeUnit unit) {
        try {
            // 使用 Lua 脚本实现原子性操作
            String script = "if redis.call('get', KEYS[1]) == false then " +
                           "redis.call('set', KEYS[1], ARGV[1]) " +
                           "redis.call('expire', KEYS[1], ARGV[2]) " +
                           "return 1 " +
                           "else " +
                           "return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = redisTemplate.execute(redisScript, 
                    Collections.singletonList(lockKey), 
                    Thread.currentThread().getId() + "", 
                    String.valueOf(unit.toSeconds(timeout)));
            
            return result != null && result == 1;
        } catch (Exception e) {
            log.error("获取分布式锁异常: lockKey={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     */
    private void releaseLock(String lockKey) {
        try {
            // 使用 Lua 脚本确保只释放自己持有的锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('del', KEYS[1]) " +
                           "else " +
                           "return 0 " +
                           "end";
            
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            redisTemplate.execute(redisScript, 
                    Collections.singletonList(lockKey), 
                    Thread.currentThread().getId() + "");
        } catch (Exception e) {
            log.error("释放分布式锁异常: lockKey={}", lockKey, e);
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     */
    public void delete(List<String> keys) {
        redisTemplate.delete(keys);
    }

    /**
     * 判断缓存是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public void expire(String key, long expireSeconds) {
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
    }

    /**
     * 获取过期时间
     */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire != null ? expire : -1;
    }

    /**
     * 根据模式删除缓存
     */
    public void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 递增
     */
    public long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     */
    public long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}

