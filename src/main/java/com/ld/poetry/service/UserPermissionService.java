package com.ld.poetry.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ld.poetry.entity.UserPermission;

import java.util.List;

public interface UserPermissionService {
    UserPermission getById(Integer id);
    List<UserPermission> getAll();
    boolean save(UserPermission userPermission);
    boolean update(UserPermission userPermission);
    boolean delete(Integer id);
    
    // 多条件查询方法
    List<UserPermission> findByConditions(QueryWrapper<UserPermission> queryWrapper);
}
