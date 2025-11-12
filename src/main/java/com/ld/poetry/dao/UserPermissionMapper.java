package com.ld.poetry.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ld.poetry.entity.UserPermission;

import java.util.List;

public interface UserPermissionMapper extends BaseMapper<UserPermission> {
    List<UserPermission> selectByConditions(QueryWrapper<UserPermission> queryWrapper);
}
