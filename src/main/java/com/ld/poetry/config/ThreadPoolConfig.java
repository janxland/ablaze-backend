package com.ld.poetry.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 * 优化线程池参数，防止线程池资源耗尽和任务堆积
 * 
 * 线程池参数设计原则：
 * 1. corePoolSize: 核心线程数，根据 CPU 核心数设置（CPU密集型：N+1，IO密集型：2N）
 * 2. maxPoolSize: 最大线程数，根据业务并发量设置
 * 3. queueCapacity: 队列容量，防止任务堆积导致内存溢出
 * 4. keepAliveSeconds: 线程空闲时间，回收多余线程
 * 5. rejectionPolicy: 拒绝策略，处理超出处理能力的任务
 */
@Configuration
@Slf4j
public class ThreadPoolConfig implements AsyncConfigurer {

    /**
     * 获取 CPU 核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 核心线程数：CPU 核心数 + 1（适合 CPU 密集型任务）
     * 如果是 IO 密集型，可以设置为 2 * CPU_COUNT
     */
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    /**
     * 最大线程数：核心线程数的 2 倍
     */
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE * 2;

    /**
     * 队列容量：根据业务需求设置，防止任务堆积
     */
    private static final int QUEUE_CAPACITY = 200;

    /**
     * 线程空闲时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 线程名前缀
     */
    private static final String THREAD_NAME_PREFIX = "poetry-async-";

    /**
     * 异步任务线程池
     * 用于处理异步任务，如邮件发送、消息推送等
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(CORE_POOL_SIZE);
        // 最大线程数
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        // 队列容量
        executor.setQueueCapacity(QUEUE_CAPACITY);
        // 线程空闲时间
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        // 线程名前缀
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        
        // 拒绝策略：调用者运行策略（由调用线程执行该任务）
        // 其他可选策略：
        // - AbortPolicy: 直接抛出异常（默认）
        // - DiscardPolicy: 直接丢弃任务
        // - DiscardOldestPolicy: 丢弃最老的任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("异步任务线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
        return executor;
    }

    /**
     * 缓存刷新线程池
     * 用于处理缓存刷新任务，独立线程池避免影响主业务
     */
    @Bean(name = "cacheRefreshExecutor")
    public Executor cacheRefreshExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("cache-refresh-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("缓存刷新线程池初始化完成");
        
        return executor;
    }

    /**
     * 邮件发送线程池
     * 用于处理邮件发送任务
     */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mail-send-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("邮件发送线程池初始化完成");
        
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor();
    }
}

