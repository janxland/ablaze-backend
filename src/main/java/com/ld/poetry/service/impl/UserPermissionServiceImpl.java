package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.entity.UserPermission;
import com.ld.poetry.dao.UserPermissionMapper;
import com.ld.poetry.service.UserPermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPermissionServiceImpl extends ServiceImpl<UserPermissionMapper, UserPermission> implements UserPermissionService {

    @Override
    public UserPermission getById(Integer id) {
        return getById(id);
    }

    @Override
    public List<UserPermission> getAll() {
        return list();
    }

    @Override
    public boolean save(UserPermission userPermission) {
        return save(userPermission);
    }

    @Override
    public boolean update(UserPermission userPermission) {
        return updateById(userPermission);
    }

    @Override
    public boolean delete(Integer id) {
        return removeById(id);
    }

    @Override
    public List<UserPermission> findByConditions(QueryWrapper<UserPermission> queryWrapper) {
        return baseMapper.selectByConditions(queryWrapper);
    }
}
