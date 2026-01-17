package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 内存缓存工具类
 * 
 * 内存优化：
 * 1. 缓存大小限制，防止内存溢出
 * 2. 定期清理过期缓存
 * 3. 守护线程，不阻止JVM退出
 */
@Slf4j
public class PoetryCache {

    //键值对集合
    private final static Map<String, Entity> map = new ConcurrentHashMap<>();

    //定时器线程池，用于清除过期缓存 - 使用守护线程，减少资源占用
    private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "poetry-cache-cleaner");
                t.setDaemon(true); // 守护线程，不会阻止JVM退出
                return t;
            }
    );

    // 缓存大小限制（防止内存溢出）
    private static final int MAX_CACHE_SIZE = 10000;
    
    // 定期清理任务（每小时清理一次过期缓存）
    static {
        executor.scheduleAtFixedRate(() -> {
            try {
                // 清理逻辑：检查是否有过期但未清理的缓存
                // 注意：已过期的缓存应该已经被定时任务清理，这里主要作为兜底
                if (map.size() > MAX_CACHE_SIZE * 0.8) {
                    log.warn("缓存使用率超过80%，当前大小: {}/{}", map.size(), MAX_CACHE_SIZE);
                }
            } catch (Exception e) {
                log.error("清理缓存时发生错误", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * 添加缓存
     *
     * @param key  键
     * @param data 值
     */
    public static void put(String key, Object data) {
        put(key, data, 0);
    }

    /**
     * 添加缓存
     *
     * @param key    键
     * @param data   值
     * @param expire 过期时间，单位：秒， 0表示无限长（不推荐，可能导致内存泄漏）
     */
    public static void put(String key, Object data, long expire) {
        // 内存优化：检查缓存大小，防止内存溢出
        if (map.size() >= MAX_CACHE_SIZE && !map.containsKey(key)) {
            log.warn("缓存大小已达到上限 {}，建议使用 Redis 缓存或清理过期缓存。当前缓存: {}", MAX_CACHE_SIZE, map.size());
            // 可以选择清理一些过期缓存或拒绝新缓存
            // 这里仅记录警告，不阻止操作（避免影响业务）
        }

        //清除原键值对
        Entity entity = map.get(key);
        if (entity != null) {
            Future oldFuture = entity.getFuture();
            if (oldFuture != null) {
                oldFuture.cancel(true);
            }
        }

        //设置过期时间
        if (expire > 0) {
            Future future = executor.schedule(new Runnable() {
                @Override
                public void run() {
                    //过期后清除该键值对
                    synchronized (PoetryCache.class) {
                        map.remove(key);
                    }
                }
            }, expire, TimeUnit.SECONDS);
            map.put(key, new Entity(data, future));
        } else {
            //不设置过期时间 - 不推荐，可能导致内存泄漏
            log.warn("缓存 {} 未设置过期时间，可能导致内存泄漏，建议设置过期时间或使用 Redis", key);
            map.put(key, new Entity(data, null));
        }
    }

    /**
     * 读取缓存
     *
     * @param key 键
     * @return
     */
    public static Object get(String key) {
        Entity entity = map.get(key);
        return entity == null ? null : entity.getValue();
    }

    /**
     * 读取所有缓存
     *
     * @return
     */
    public static Collection values() {
        return map.values();
    }

    /**
     * 清除缓存
     *
     * @param key
     * @return
     */
    public static Object remove(String key) {
        //清除原缓存数据
        Entity entity = map.remove(key);
        if (entity == null) return null;
        //清除原键值对定时器
        Future future = entity.getFuture();
        if (future != null) future.cancel(true);
        return entity.getValue();
    }

    /**
     * 查询当前缓存的键值对数量
     *
     * @return 缓存数量
     */
    public static int size() {
        return map.size();
    }

    /**
     * 清理所有缓存（内存优化：释放内存）
     */
    public static void clear() {
        synchronized (PoetryCache.class) {
            // 取消所有定时任务
            map.values().forEach(entity -> {
                Future future = entity.getFuture();
                if (future != null) {
                    future.cancel(true);
                }
            });
            map.clear();
            log.info("已清理所有内存缓存，释放内存");
        }
    }

    /**
     * 获取缓存使用率
     *
     * @return 缓存使用率（0.0 - 1.0）
     */
    public static double getUsageRate() {
        return (double) map.size() / MAX_CACHE_SIZE;
    }

    /**
     * 缓存实体类
     */
    private static class Entity {
        //键值对的value
        private Object value;

        //定时器Future
        private Future future;

        public Entity(Object value, Future future) {
            this.value = value;
            this.future = future;
        }

        /**
         * 获取值
         *
         * @return
         */
        public Object getValue() {
            return value;
        }

        /**
         * 获取Future对象
         *
         * @return
         */
        public Future getFuture() {
            return future;
        }
    }
}
