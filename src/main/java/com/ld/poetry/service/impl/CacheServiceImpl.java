package com.ld.poetry.service.impl;

import com.ld.poetry.service.CacheService;
import com.ld.poetry.utils.RedisCacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分布式缓存服务实现类
 */
@Service
@Slf4j
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisCacheUtil.get(key);
        if (value == null) {
            return null;
        }
        try {
            return clazz.cast(value);
        } catch (ClassCastException e) {
            log.error("缓存数据类型转换异常: key={}, expectedType={}", key, clazz.getName(), e);
            return null;
        }
    }

    @Override
    public void set(String key, Object value, long expireSeconds) {
        redisCacheUtil.set(key, value, expireSeconds);
    }

    @Override
    public void delete(String key) {
        redisCacheUtil.delete(key);
    }

    @Override
    public void warmUp() {
        log.info("开始缓存预热...");
        // TODO: 实现缓存预热逻辑
        // 1. 加载热点文章
        // 2. 加载热门用户
        // 3. 加载系统配置
        log.info("缓存预热完成");
    }

    @Override
    public void refresh(String key) {
        redisCacheUtil.delete(key);
        log.info("刷新缓存: key={}", key);
    }
}

