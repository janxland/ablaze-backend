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
        lambdaUpdate().eq(Todolist::getId, id)
        .eq(Todolist::getExecutor,userId)
        .remove();
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
        LambdaQueryWrapper<Todolist> queryWrapper = new LambdaQueryWrapper<Todolist>()
                .ge(Todolist::getStartTime, todolistVO.getStartTime())
                .le(Todolist::getEndTime, todolistVO.getEndTime());
        List<Todolist> todolistList = list(queryWrapper.orderByDesc(Todolist::getCreatedAt));
        return PoetryResult.success(todolistList);
    }

    @Override
    public PoetryResult<Todolist> getTaskById(Integer id, Boolean flag, String password) {
        
        throw new UnsupportedOperationException("Unimplemented method 'getTaskById'");
    }

}
