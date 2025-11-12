package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;

/**
 * PoetryCache Redis 适配器
 * 
 * 为了平滑迁移，创建一个适配器类，保持与原有 PoetryCache 相同的接口
 * 这样现有代码无需修改即可使用 Redis 缓存
 * 
 * 使用方式：
 * 1. 在需要的地方注入 PoetryCacheRedisAdapter
 * 2. 逐步替换 PoetryCache 的调用
 * 3. 最终移除 PoetryCache 类
 */
@Component
@Slf4j
public class PoetryCacheRedisAdapter {

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 是否启用 Redis（可以通过配置控制）
     */
    private static boolean useRedis = true;

    @PostConstruct
    public void init() {
        log.info("PoetryCache Redis 适配器初始化完成，使用 Redis: {}", useRedis);
    }

    /**
     * 添加缓存（兼容原有接口）
     *
     * @param key  键
     * @param data 值
     */
    public void put(String key, Object data) {
        put(key, data, 0);
    }

    /**
     * 添加缓存（兼容原有接口）
     *
     * @param key    键
     * @param data   值
     * @param expire 过期时间，单位：秒， 0表示无限长（实际设置为30天）
     */
    public void put(String key, Object data, long expire) {
        if (useRedis) {
            if (expire > 0) {
                redisCacheUtil.set(key, data, expire);
            } else {
                // 0 表示永不过期，但 Redis 不推荐永不过期，设置为30天
                redisCacheUtil.set(key, data, 30 * 24 * 3600);
            }
        } else {
            // 降级到原有 PoetryCache
            PoetryCache.put(key, data, expire);
        }
    }

    /**
     * 读取缓存（兼容原有接口）
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        if (useRedis) {
            return redisCacheUtil.get(key);
        } else {
            return PoetryCache.get(key);
        }
    }

    /**
     * 读取所有缓存（兼容原有接口）
     * 
     * 注意：Redis 不支持直接获取所有值，此方法返回空集合
     * 如果需要此功能，建议使用 keys 模式查询
     *
     * @return 值集合
     */
    public Collection values() {
        log.warn("Redis 不支持 values() 方法，返回空集合。建议使用 keys 模式查询");
        return java.util.Collections.emptyList();
    }

    /**
     * 清除缓存（兼容原有接口）
     *
     * @param key 键
     * @return 被删除的值
     */
    public Object remove(String key) {
        if (useRedis) {
            Object value = redisCacheUtil.get(key);
            redisCacheUtil.delete(key);
            return value;
        } else {
            return PoetryCache.remove(key);
        }
    }

    /**
     * 查询当前缓存的键值对数量（兼容原有接口）
     * 
     * 注意：Redis 获取所有 key 的性能较低，不建议频繁调用
     *
     * @return 数量
     */
    public int size() {
        if (useRedis) {
            log.warn("Redis size() 方法性能较低，不建议频繁调用");
            // Redis 获取所有 key 的性能较低，这里返回 -1 表示不支持
            return -1;
        } else {
            return PoetryCache.size();
        }
    }

    /**
     * 设置是否使用 Redis
     */
    public static void setUseRedis(boolean useRedis) {
        PoetryCacheRedisAdapter.useRedis = useRedis;
    }

    /**
     * 是否使用 Redis
     */
    public static boolean isUseRedis() {
        return useRedis;
    }
}

