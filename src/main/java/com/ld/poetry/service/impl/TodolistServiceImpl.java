package com.ld.poetry.service.impl;

import com.ld.poetry.entity.Todolist;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.TodolistMapper;
import com.ld.poetry.service.TodolistService;
import com.ld.poetry.utils.PoetryUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author sara
 * @since 2023-07-09
 */
@Service
public class TodolistServiceImpl extends ServiceImpl<TodolistMapper, Todolist> implements TodolistService {

    @Override
    public PoetryResult saveTask(Todolist todolistVO) {
        save(todolistVO);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult deleteTask(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        if (userId != null) {
            lambdaUpdate().eq(Todolist::getId, id)
                    .eq(Todolist::getExecutor, userId.toString())
                    .remove();
        }
        return PoetryResult.success();
    }

    @Override
    public PoetryResult updateTask(Todolist todolistVO) {
        boolean success = updateById(todolistVO);
        if (success) {
            Todolist updatedTodolist = getById(todolistVO.getId());
            return new PoetryResult(updatedTodolist);
        } else {
            throw new RuntimeException("Failed to update task");
        }
    }

    @Override
    public PoetryResult<List<Todolist>> listTask(Todolist todolistVO) {
        LambdaQueryWrapper<Todolist> queryWrapper = buildQueryWrapper(todolistVO);
        List<Todolist> todolistList = list(queryWrapper.orderByDesc(Todolist::getCreatedAt));
        return PoetryResult.success(todolistList);
    }

    /**
     * 构建查询条件 - 扁平化、低耦合的查询逻辑
     */
    private LambdaQueryWrapper<Todolist> buildQueryWrapper(Todolist todolistVO) {
        LambdaQueryWrapper<Todolist> queryWrapper = new LambdaQueryWrapper<>();
        
        // 执行者过滤：如果查询条件中提供了 executor，使用它；否则默认使用当前用户
        applyExecutorFilter(queryWrapper, todolistVO.getExecutor());
        
        // 状态过滤：支持查询已完成或未完成的任务
        applyStatusFilter(queryWrapper, todolistVO.getStatus());
        
        // 时间范围过滤：查询时间范围有交集的任务
        applyTimeRangeFilter(queryWrapper, todolistVO.getStartTime(), todolistVO.getEndTime());
        
        // 其他字段过滤
        applyOtherFilters(queryWrapper, todolistVO);
        
        return queryWrapper;
    }

    /**
     * 应用执行者过滤条件
     */
    private void applyExecutorFilter(LambdaQueryWrapper<Todolist> queryWrapper, String executor) {
        if (executor != null && !executor.isEmpty()) {
            queryWrapper.eq(Todolist::getExecutor, executor);
        } else {
            // 如果查询条件中没有提供 executor，默认查询当前用户的任务
            Integer userId = PoetryUtil.getUserId();
            if (userId != null) {
                queryWrapper.eq(Todolist::getExecutor, userId.toString());
            }
        }
    }

    /**
     * 应用状态过滤条件
     */
    private void applyStatusFilter(LambdaQueryWrapper<Todolist> queryWrapper, String status) {
        if (status == null || status.isEmpty()) {
            return;
        }
        
        // 支持查询未完成的任务（todo 或 doing）
        if ("undone".equalsIgnoreCase(status) || "unfinished".equalsIgnoreCase(status)) {
            queryWrapper.and(wrapper -> wrapper
                .eq(Todolist::getStatus, "todo")
                .or()
                .eq(Todolist::getStatus, "doing")
            );
        } else {
            // 精确匹配状态
            queryWrapper.eq(Todolist::getStatus, status);
        }
    }

    /**
     * 应用时间范围过滤条件
     * 查询逻辑：任务的时间范围与查询时间范围有交集
     * - 如果提供了 startTime，查询任务的 endTime >= startTime（任务结束时间在查询开始时间之后）或 endTime 为 null
     * - 如果提供了 endTime，查询任务的 startTime <= endTime（任务开始时间在查询结束时间之前）或 startTime 为 null
     * 这样可以查询到时间范围有交集的任务，同时不排除未设置时间的任务
     */
    private void applyTimeRangeFilter(LambdaQueryWrapper<Todolist> queryWrapper, 
                                     java.time.LocalDateTime startTime, 
                                     java.time.LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            // 两个时间都提供：查询时间范围有交集的任务
            queryWrapper.and(wrapper -> wrapper
                .and(innerWrapper -> innerWrapper
                    .ge(Todolist::getEndTime, startTime)
                    .or()
                    .isNull(Todolist::getEndTime)
                )
                .and(innerWrapper -> innerWrapper
                    .le(Todolist::getStartTime, endTime)
                    .or()
                    .isNull(Todolist::getStartTime)
                )
            );
        } else if (startTime != null) {
            // 只提供开始时间：查询结束时间在开始时间之后的任务
            queryWrapper.and(wrapper -> wrapper
                .ge(Todolist::getEndTime, startTime)
                .or()
                .isNull(Todolist::getEndTime)
            );
        } else if (endTime != null) {
            // 只提供结束时间：查询开始时间在结束时间之前的任务
            queryWrapper.and(wrapper -> wrapper
                .le(Todolist::getStartTime, endTime)
                .or()
                .isNull(Todolist::getStartTime)
            );
        }
    }

    /**
     * 应用其他字段过滤条件
     */
    private void applyOtherFilters(LambdaQueryWrapper<Todolist> queryWrapper, Todolist todolistVO) {
        if (todolistVO.getId() != null) {
            queryWrapper.eq(Todolist::getId, todolistVO.getId());
        }
        
        if (todolistVO.getTitle() != null && !todolistVO.getTitle().isEmpty()) {
            queryWrapper.like(Todolist::getTitle, todolistVO.getTitle());
        }
        
        if (todolistVO.getUrgency() != null) {
            queryWrapper.eq(Todolist::getUrgency, todolistVO.getUrgency());
        }
        
        if (todolistVO.getDependencyId() != null) {
            queryWrapper.eq(Todolist::getDependencyId, todolistVO.getDependencyId());
        }
    }

    @Override
    public PoetryResult<Todolist> getTaskById(Integer id, Boolean flag, String password) {
        
        throw new UnsupportedOperationException("Unimplemented method 'getTaskById'");
    }

}
