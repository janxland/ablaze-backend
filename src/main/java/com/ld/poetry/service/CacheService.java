package com.ld.poetry.service;

import com.ld.poetry.utils.RedisCacheUtil;

/**
 * 分布式缓存服务接口
 * 
 * 分布式缓存架构设计要点：
 * 1. 缓存分层：
 *    - L1: 本地缓存（Caffeine/Guava Cache）- 热点数据，快速访问
 *    - L2: Redis 缓存 - 分布式共享缓存
 *    - L3: 数据库 - 持久化存储
 * 
 * 2. 缓存更新策略：
 *    - Cache Aside（旁路缓存）：先更新数据库，再删除缓存
 *    - Write Through（写透）：先更新缓存，再更新数据库
 *    - Write Back（写回）：先更新缓存，异步更新数据库
 * 
 * 3. 缓存一致性：
 *    - 最终一致性：允许短暂不一致，通过过期时间保证最终一致
 *    - 强一致性：使用分布式锁保证读写一致性
 * 
 * 4. 缓存预热：
 *    - 系统启动时加载热点数据到缓存
 *    - 定时任务刷新热点数据
 * 
 * 5. 缓存降级：
 *    - 缓存不可用时，直接访问数据库
 *    - 使用熔断机制，防止数据库压力过大
 */
public interface CacheService {

    /**
     * 获取缓存数据
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 设置缓存数据
     */
    void set(String key, Object value, long expireSeconds);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 缓存预热（加载热点数据）
     */
    void warmUp();

    /**
     * 刷新缓存
     */
    void refresh(String key);
}

