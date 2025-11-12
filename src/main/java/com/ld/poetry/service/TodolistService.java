package com.ld.poetry.service;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Todolist;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author sara
 * @since 2023-07-09
 */
public interface TodolistService extends IService<Todolist> {

    PoetryResult saveTask(Todolist todolistVO);

    PoetryResult deleteTask(Integer id);

    PoetryResult updateTask(Todolist todolistVO);

    PoetryResult<List<Todolist>> listTask(Todolist todolistVO);

    PoetryResult<Todolist> getTaskById(Integer id, Boolean flag, String password);

}
