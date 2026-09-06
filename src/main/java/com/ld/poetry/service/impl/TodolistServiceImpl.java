package com.ld.poetry.service.impl;

import com.ld.poetry.entity.Todolist;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.TodolistMapper;
import com.ld.poetry.service.TodolistService;
import com.ld.poetry.utils.PoetryUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class TodolistServiceImpl extends ServiceImpl<TodolistMapper, Todolist> implements TodolistService {

    @Override
    public PoetryResult saveTask(Todolist todolistVO) {
        // 数据归属兜底：executor 为空/无效（如历史遗留的 "0"）时强制写当前登录用户，
        // 避免出现查不到的孤儿任务（历史上 30 条 executor='0' 的教训）
        Integer userId = PoetryUtil.getUserId();
        if (userId != null) {
            String executor = todolistVO.getExecutor();
            if (executor == null || executor.trim().isEmpty() || "0".equals(executor.trim())) {
                todolistVO.setExecutor(userId.toString());
            }
        }
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
        // 扁平化处理：提前返回，减少嵌套
        boolean success = updateById(todolistVO);
        if (!success) {
            throw new RuntimeException("Failed to update task");
        }
        
        Todolist updatedTodolist = getById(todolistVO.getId());
        return PoetryResult.success(updatedTodolist);
    }

    @Override
    public PoetryResult<List<Todolist>> listTask(Todolist todolistVO) {
        // 防止空指针异常
        if (todolistVO == null) {
            todolistVO = new Todolist();
        }
        
        LambdaQueryWrapper<Todolist> queryWrapper = buildQueryWrapper(todolistVO);
        List<Todolist> todolistList = list(queryWrapper.orderByDesc(Todolist::getCreatedAt));
        
        // 调试日志：记录查询条件和结果数量
        log.debug("查询任务列表 - executor: {}, status: {}, startTime: {}, endTime: {}, 结果数量: {}", 
                todolistVO.getExecutor(), todolistVO.getStatus(), 
                todolistVO.getStartTime(), todolistVO.getEndTime(), 
                todolistList != null ? todolistList.size() : 0);
        
        return PoetryResult.success(todolistList);
    }

    /**
     * 构建查询条件 - 扁平化、低耦合的查询逻辑
     */
    private LambdaQueryWrapper<Todolist> buildQueryWrapper(Todolist todolistVO) {
        LambdaQueryWrapper<Todolist> queryWrapper = new LambdaQueryWrapper<>();
        
        // 扁平化处理：每个过滤条件独立处理，避免嵌套
        String executor = todolistVO != null ? todolistVO.getExecutor() : null;
        String status = todolistVO != null ? todolistVO.getStatus() : null;
        java.time.LocalDateTime startTime = todolistVO != null ? todolistVO.getStartTime() : null;
        java.time.LocalDateTime endTime = todolistVO != null ? todolistVO.getEndTime() : null;
        
        applyExecutorFilter(queryWrapper, executor);
        applyStatusFilter(queryWrapper, status);
        applyTimeRangeFilter(queryWrapper, startTime, endTime);
        applyOtherFilters(queryWrapper, todolistVO);
        
        return queryWrapper;
    }

    /**
     * 应用执行者过滤条件（扁平化、低耦合）
     */
    private void applyExecutorFilter(LambdaQueryWrapper<Todolist> queryWrapper, String executor) {
        // 扁平化处理：提前返回，减少嵌套
        if (hasText(executor)) {
            queryWrapper.eq(Todolist::getExecutor, executor.trim());
            log.debug("使用提供的 executor: {}", executor.trim());
            return;
        }
        
        // 未提供 executor，使用当前用户ID
        Integer userId = PoetryUtil.getUserId();
        if (userId != null) {
            queryWrapper.eq(Todolist::getExecutor, userId.toString());
            log.debug("使用当前用户ID作为 executor: {}", userId);
        } else {
            log.debug("未提供 executor 且用户未登录，不添加 executor 过滤条件");
        }
    }
    
    /**
     * 字符串非空检查（扁平化处理）
     */
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * 应用状态过滤条件（扁平化处理）
     */
    private void applyStatusFilter(LambdaQueryWrapper<Todolist> queryWrapper, String status) {
        if (!hasText(status)) {
            return;
        }
        
        // 扁平化处理：提前返回，减少嵌套
        if (isUndoneStatus(status)) {
            queryWrapper.and(wrapper -> wrapper
                .eq(Todolist::getStatus, "todo")
                .or()
                .eq(Todolist::getStatus, "doing")
            );
            return;
        }
        
        // 精确匹配状态
        queryWrapper.eq(Todolist::getStatus, status);
    }
    
    /**
     * 判断是否为未完成状态（扁平化处理）
     */
    private boolean isUndoneStatus(String status) {
        return "undone".equalsIgnoreCase(status) || "unfinished".equalsIgnoreCase(status);
    }

    /**
     * 应用时间范围过滤条件（扁平化、低耦合）
     */
    private void applyTimeRangeFilter(LambdaQueryWrapper<Todolist> queryWrapper, 
                                     java.time.LocalDateTime startTime, 
                                     java.time.LocalDateTime endTime) {
        // 扁平化处理：提前返回，减少嵌套
        if (startTime == null && endTime == null) {
            return;
        }
        
        // 扁平化处理：分别处理不同情况
        if (startTime != null && endTime != null) {
            applyTimeRangeBoth(queryWrapper, startTime, endTime);
        } else if (startTime != null) {
            applyTimeRangeStartOnly(queryWrapper, startTime);
        } else {
            applyTimeRangeEndOnly(queryWrapper, endTime);
        }
    }
    
    /**
     * 应用时间范围（两个时间都提供）（扁平化处理）
     */
    private void applyTimeRangeBoth(LambdaQueryWrapper<Todolist> queryWrapper, 
                                   java.time.LocalDateTime startTime, 
                                   java.time.LocalDateTime endTime) {
        queryWrapper.and(wrapper -> wrapper
            .and(innerWrapper -> innerWrapper
                .le(Todolist::getStartTime, endTime)
                .or()
                .isNull(Todolist::getStartTime)
            )
            .and(innerWrapper -> innerWrapper
                .ge(Todolist::getEndTime, startTime)
                .or()
                .isNull(Todolist::getEndTime)
            )
        );
    }
    
    /**
     * 应用时间范围（只提供开始时间）（扁平化处理）
     */
    private void applyTimeRangeStartOnly(LambdaQueryWrapper<Todolist> queryWrapper, 
                                        java.time.LocalDateTime startTime) {
        queryWrapper.and(wrapper -> wrapper
            .ge(Todolist::getEndTime, startTime)
            .or()
            .isNull(Todolist::getEndTime)
        );
    }
    
    /**
     * 应用时间范围（只提供结束时间）（扁平化处理）
     */
    private void applyTimeRangeEndOnly(LambdaQueryWrapper<Todolist> queryWrapper, 
                                      java.time.LocalDateTime endTime) {
        queryWrapper.and(wrapper -> wrapper
            .le(Todolist::getStartTime, endTime)
            .or()
            .isNull(Todolist::getStartTime)
        );
    }

    /**
     * 应用其他字段过滤条件（扁平化、低耦合）
     */
    private void applyOtherFilters(LambdaQueryWrapper<Todolist> queryWrapper, Todolist todolistVO) {
        if (todolistVO == null) {
            return;
        }
        
        // 扁平化处理：每个条件独立处理，提前返回
        if (todolistVO.getId() != null) {
            queryWrapper.eq(Todolist::getId, todolistVO.getId());
        }
        
        if (hasText(todolistVO.getTitle())) {
            queryWrapper.like(Todolist::getTitle, todolistVO.getTitle().trim());
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
        // 扁平化处理：参数校验提前返回
        if (id == null) {
            return PoetryResult.fail("任务ID不能为空");
        }
        
        Todolist task = getById(id);
        if (task == null) {
            return PoetryResult.fail("任务不存在");
        }
        
        // 扁平化处理：根据flag判断是否需要密码验证
        if (flag != null && !flag) {
            // 需要密码验证
            if (password == null || password.isEmpty()) {
                return PoetryResult.fail("请输入任务密码");
            }
            // 这里可以根据实际需求添加密码验证逻辑
            // 如果Todolist实体有password字段，可以验证
        }
        
        return PoetryResult.success(task);
    }

}
