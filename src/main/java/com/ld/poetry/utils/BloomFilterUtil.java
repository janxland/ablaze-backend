package com.ld.poetry.utils;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 布隆过滤器工具类
 * 
 * 布隆过滤器原理：
 * 1. 使用多个哈希函数将元素映射到位数组的多个位置
 * 2. 查询时，如果所有位置都是1，则可能存在；如果有0，则一定不存在
 * 3. 优点：空间效率高，查询速度快
 * 4. 缺点：存在误判率（可能将不存在的元素判断为存在），但不会将存在的元素判断为不存在
 * 
 * 应用场景：
 * - 防止缓存穿透：在查询缓存前先判断数据是否存在
 * - 去重：判断元素是否已存在
 * - 爬虫URL去重
 * 
 * 与缓存空对象和互斥锁的比较：
 * 1. 布隆过滤器：空间占用小，查询快，但存在误判率，适合大规模数据
 * 2. 缓存空对象：实现简单，但占用缓存空间，可能缓存大量无效数据
 * 3. 互斥锁：保证数据一致性，但可能造成线程阻塞，影响性能
 * 
 * 最佳实践：结合使用
 * - 布隆过滤器：第一层防护，快速过滤不存在的请求
 * - 缓存空对象：第二层防护，减少数据库查询
 * - 互斥锁：第三层防护，防止缓存击穿
 */
@Component
@Slf4j
public class BloomFilterUtil {

    /**
     * 预期插入的数据量
     */
    private static final long EXPECTED_INSERTIONS = 1000000L;

    /**
     * 误判率（0.01 表示 1%）
     */
    private static final double FALSE_POSITIVE_PROBABILITY = 0.01;

    /**
     * 文章ID布隆过滤器
     */
    private BloomFilter<String> articleBloomFilter;

    /**
     * 用户ID布隆过滤器
     */
    private BloomFilter<String> userBloomFilter;

    /**
     * 评论ID布隆过滤器
     */
    private BloomFilter<String> commentBloomFilter;

    /**
     * 存储各个业务模块的布隆过滤器
     */
    private final ConcurrentHashMap<String, BloomFilter<String>> bloomFilterMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化各个业务模块的布隆过滤器
        articleBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );
        
        userBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );
        
        commentBloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );

        log.info("布隆过滤器初始化完成 - 预期插入量: {}, 误判率: {}", EXPECTED_INSERTIONS, FALSE_POSITIVE_PROBABILITY);
    }

    /**
     * 添加文章ID到布隆过滤器
     */
    public void addArticleId(String articleId) {
        articleBloomFilter.put("article:" + articleId);
    }

    /**
     * 判断文章ID是否存在
     */
    public boolean mightContainArticleId(String articleId) {
        return articleBloomFilter.mightContain("article:" + articleId);
    }

    /**
     * 添加用户ID到布隆过滤器
     */
    public void addUserId(String userId) {
        userBloomFilter.put("user:" + userId);
    }

    /**
     * 判断用户ID是否存在
     */
    public boolean mightContainUserId(String userId) {
        return userBloomFilter.mightContain("user:" + userId);
    }

    /**
     * 添加评论ID到布隆过滤器
     */
    public void addCommentId(String commentId) {
        commentBloomFilter.put("comment:" + commentId);
    }

    /**
     * 判断评论ID是否存在
     */
    public boolean mightContainCommentId(String commentId) {
        return commentBloomFilter.mightContain("comment:" + commentId);
    }

    /**
     * 获取或创建指定业务模块的布隆过滤器
     */
    public BloomFilter<String> getOrCreateBloomFilter(String moduleName) {
        return bloomFilterMap.computeIfAbsent(moduleName, k -> {
            BloomFilter<String> filter = BloomFilter.create(
                    Funnels.stringFunnel(Charset.defaultCharset()),
                    EXPECTED_INSERTIONS,
                    FALSE_POSITIVE_PROBABILITY
            );
            log.info("创建布隆过滤器: {}", moduleName);
            return filter;
        });
    }

    /**
     * 添加数据到指定业务模块的布隆过滤器
     */
    public void add(String moduleName, String key) {
        BloomFilter<String> filter = getOrCreateBloomFilter(moduleName);
        filter.put(moduleName + ":" + key);
    }

    /**
     * 判断指定业务模块的数据是否存在
     */
    public boolean mightContain(String moduleName, String key) {
        BloomFilter<String> filter = bloomFilterMap.get(moduleName);
        if (filter == null) {
            return false;
        }
        return filter.mightContain(moduleName + ":" + key);
    }
}

