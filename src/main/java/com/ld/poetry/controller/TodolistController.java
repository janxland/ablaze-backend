package com.ld.poetry.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Todolist;
import com.ld.poetry.service.TodolistService;
import com.ld.poetry.utils.CommonConst;
import com.ld.poetry.utils.PoetryCache;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.vo.BaseRequestVO;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author sara
 * @since 2023-07-09
 */
@RestController
@RequestMapping("/todolist")
public class TodolistController {

    @Autowired
    private TodolistService todolistService;

    /**
     * 保存任务
     */
    @RequirePermission(PermissionCode.USER_ADMIN)
    @PostMapping("/saveTask")
    public PoetryResult saveTask(@Validated @RequestBody Todolist todolistVO) {
        // PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        return todolistService.saveTask(todolistVO);
    }


    /**
     * 删除任务
     */
    @GetMapping("/deleteTask")
    @RequirePermission(PermissionCode.USER_ADMIN)
    public PoetryResult deleteTask(@RequestParam("id") Integer id) {
        PoetryCache.remove(CommonConst.USER_ARTICLE_LIST + PoetryUtil.getUserId().toString());
        return todolistService.deleteTask(id);
    }


    /**
     * 更新任务
     */
    @PostMapping("/updateTask")
    @RequirePermission(PermissionCode.USER_ADMIN)
    public PoetryResult updateTask(@Validated @RequestBody Todolist todolistVO) {
        return todolistService.updateTask(todolistVO);
    }


    /**
     * 查询任务List
     */
    @PostMapping("/listTask")
    @RequirePermission(PermissionCode.PUBLIC)
    public PoetryResult<List<Todolist>> listTask(@RequestBody Todolist todolistVO) {
        return todolistService.listTask(todolistVO);
    }

    /**
     * 查询任务
     * <p>
     * flag = true：查询可见的任务
     */
    @GetMapping("/getTaskById")
    @RequirePermission(PermissionCode.PUBLIC)
    public PoetryResult<Todolist> getTaskById(@RequestParam("id") Integer id, @RequestParam("flag") Boolean flag, @RequestParam(value = "password", required = false) String password) {
        return todolistService.getTaskById(id, flag, password);
    }
}

