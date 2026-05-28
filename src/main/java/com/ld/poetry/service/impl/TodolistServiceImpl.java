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
        if (!updateById(todolistVO)) {
            throw new RuntimeException("更新任务失败");
        }
        return PoetryResult.success(getById(todolistVO.getId()));
    }

    @Override
    public PoetryResult<List<Todolist>> listTask(Todolist todolistVO) {
        if (todolistVO == null) {
            todolistVO = new Todolist();
        }
        
        List<Todolist> todolistList = list(buildQueryWrapper(todolistVO).orderByDesc(Todolist::getCreatedAt));
        if (todolistList == null) {
            todolistList = new java.util.ArrayList<>();
        }
        
        PoetryResult<List<Todolist>> result = PoetryResult.success(todolistList);
        if (result.getData() == null) {
            result.setData(new java.util.ArrayList<>());
        }
        
        return result;
    }

    private LambdaQueryWrapper<Todolist> buildQueryWrapper(Todolist todolistVO) {
        LambdaQueryWrapper<Todolist> queryWrapper = new LambdaQueryWrapper<>();
        
        if (todolistVO == null) {
            return queryWrapper;
        }
        
        applyExecutorFilter(queryWrapper, todolistVO.getExecutor());
        applyStatusFilter(queryWrapper, todolistVO.getStatus());
        applyTimeRangeFilter(queryWrapper, todolistVO.getStartTime(), todolistVO.getEndTime());
        applyOtherFilters(queryWrapper, todolistVO);
        
        return queryWrapper;
    }

    private void applyExecutorFilter(LambdaQueryWrapper<Todolist> queryWrapper, String executor) {
        if (hasText(executor)) {
            queryWrapper.eq(Todolist::getExecutor, executor.trim());
            return;
        }
        
        Integer userId = PoetryUtil.getUserId();
        if (userId != null) {
            queryWrapper.eq(Todolist::getExecutor, userId.toString());
        }
    }
    
    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private void applyStatusFilter(LambdaQueryWrapper<Todolist> queryWrapper, String status) {
        if (!hasText(status)) {
            return;
        }
        
        if ("undone".equalsIgnoreCase(status) || "unfinished".equalsIgnoreCase(status)) {
            queryWrapper.and(w -> w.eq(Todolist::getStatus, "todo").or().eq(Todolist::getStatus, "doing"));
            return;
        }
        
        queryWrapper.eq(Todolist::getStatus, status);
    }

    private void applyTimeRangeFilter(LambdaQueryWrapper<Todolist> queryWrapper,
                                     java.time.LocalDateTime startTime,
                                     java.time.LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            return;
        }

        // 历史数据中存在 end_time < start_time 的脏数据（早期前端把字段写反过），
        // 直接按列名做 overlap 会把绝大多数任务过滤掉。
        // 这里用 LEAST/GREATEST 把两列归一成 [min, max] 区间再判断 overlap，
        // 同时允许任意一列为 NULL 的任务参与匹配。
        queryWrapper.and(w -> {
            if (startTime != null && endTime != null) {
                w.apply("(start_time IS NULL OR end_time IS NULL "
                        + "OR LEAST(start_time, end_time) <= {0})", endTime)
                 .apply("(start_time IS NULL OR end_time IS NULL "
                        + "OR GREATEST(start_time, end_time) >= {0})", startTime);
            } else if (startTime != null) {
                w.apply("(start_time IS NULL OR end_time IS NULL "
                        + "OR GREATEST(start_time, end_time) >= {0})", startTime);
            } else {
                w.apply("(start_time IS NULL OR end_time IS NULL "
                        + "OR LEAST(start_time, end_time) <= {0})", endTime);
            }
        });
    }

    private void applyOtherFilters(LambdaQueryWrapper<Todolist> queryWrapper, Todolist todolistVO) {
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
        if (id == null) {
            return PoetryResult.fail("任务ID不能为空");
        }
        
        Todolist task = getById(id);
        if (task == null) {
            return PoetryResult.fail("任务不存在");
        }
        
        if (flag != null && !flag && (password == null || password.isEmpty())) {
            return PoetryResult.fail("请输入任务密码");
        }
        
        return PoetryResult.success(task);
    }

}
