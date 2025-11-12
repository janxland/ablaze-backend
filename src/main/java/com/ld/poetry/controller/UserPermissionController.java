package com.ld.poetry.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ld.poetry.entity.UserPermission;
import com.ld.poetry.service.UserPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 用户权限
 * </p>
 *
 * @author janxland
 * @since 2024-09-26
 * <p>
 * 仅站长可以操作
 */
@RestController
@RequestMapping("/user-permissions")
public class UserPermissionController {

    @Autowired
    private UserPermissionService userPermissionService;

    @GetMapping("/{id}")
    public UserPermission getUserPermission(@PathVariable Integer id) {
        return userPermissionService.getById(id);
    }

    @GetMapping
    public List<UserPermission> getAllUserPermissions() {
        return userPermissionService.getAll();
    }

    @PostMapping
    public boolean saveUserPermission(@RequestBody UserPermission userPermission) {
        return userPermissionService.save(userPermission);
    }

    @PutMapping
    public boolean updateUserPermission(@RequestBody UserPermission userPermission) {
        return userPermissionService.update(userPermission);
    }

    @DeleteMapping("/{id}")
    public boolean deleteUserPermission(@PathVariable Integer id) {
        return userPermissionService.delete(id);
    }

    @GetMapping("/search")
    public List<UserPermission> findByConditions(@RequestParam(required = false) Integer userId,
                                                 @RequestParam(required = false) String goodCode) {
        QueryWrapper<UserPermission> queryWrapper = new QueryWrapper<>();
        if (userId != null) {
            queryWrapper.eq("user_id", userId);
        }
        if (goodCode != null) {
            queryWrapper.eq("good_code", goodCode);
        }
        return userPermissionService.findByConditions(queryWrapper);
    }
}
