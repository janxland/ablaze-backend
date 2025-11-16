package com.ld.poetry.controller;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.auth.AuthContext;
import com.ld.poetry.entity.User;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.service.UserMappingService;
import com.ld.poetry.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证测试控制器 - 展示新的权限系统
 */
@RestController
@RequestMapping("/api/auth-test")
public class AuthTestController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMappingService userMappingService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public Map<String, Object> getCurrentUser() {
        AuthContext.UserInfo user = AuthContext.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        
        if (user != null) {
            result.put("authenticated", true);
            result.put("userId", user.getUserId());
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            result.put("userType", user.getUserType());
            result.put("isAdmin", user.isAdmin());
            result.put("isSuperAdmin", user.isSuperAdmin());
        } else {
            result.put("authenticated", false);
            result.put("error", "用户信息为空");
        }
        
        return result;
    }

    /**
     * 调试接口 - 无权限检查
     */
    @GetMapping("/debug")
    public Map<String, Object> debugAuth() {
        AuthContext.UserInfo user = AuthContext.getCurrentUser();
        Map<String, Object> result = new HashMap<>();
        
        result.put("hasUser", user != null);
        if (user != null) {
            result.put("userId", user.getUserId());
            result.put("username", user.getUsername());
            result.put("email", user.getEmail());
            result.put("userType", user.getUserType());
            result.put("isAdmin", user.isAdmin());
            result.put("isSuperAdmin", user.isSuperAdmin());
        }
        
        return result;
    }

    /**
     * 测试认证中心token兼容性和用户映射
     */
    @PostMapping("/test-auth-center-token")
    public Map<String, Object> testAuthCenterToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 基础token解析
            Integer authCenterUserId = jwtUtil.getUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            String userType = jwtUtil.getUserTypeFromToken(token);
            String authSource = jwtUtil.getAuthSourceFromToken(token);
            boolean isValid = jwtUtil.validateToken(token);
            
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("valid", isValid);
            tokenInfo.put("authCenterUserId", authCenterUserId);
            tokenInfo.put("username", username);
            tokenInfo.put("email", email);
            tokenInfo.put("userType", userType);
            tokenInfo.put("authSource", authSource);
            result.put("tokenInfo", tokenInfo);

            // 2. 用户映射测试
            User localUser = userMappingService.getOrCreateLocalUser(token);
            if (localUser != null) {
                Map<String, Object> mappingInfo = new HashMap<>();
                mappingInfo.put("localUserId", localUser.getId());
                mappingInfo.put("localUsername", localUser.getUsername());
                mappingInfo.put("localEmail", localUser.getEmail());
                mappingInfo.put("localUserType", localUser.getUserType());
                mappingInfo.put("isNewUser", localUser.getCreateTime() != null);
                result.put("mappingInfo", mappingInfo);
                result.put("success", true);
                result.put("message", "认证中心token解析和用户映射成功");
            } else {
                result.put("success", false);
                result.put("message", "用户映射失败");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "认证中心token处理失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 公开接口
     */
    @GetMapping("/public")
    @RequirePermission(PermissionCode.PUBLIC)
    public Map<String, Object> publicEndpoint() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "公开接口，无需认证");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 管理员接口
     */
    @GetMapping("/admin")
    @RequirePermission(PermissionCode.USER_ADMIN)
    public Map<String, Object> adminEndpoint() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "管理员专用接口");
        result.put("userId", AuthContext.getCurrentUserId());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 超级管理员接口
     */
    @GetMapping("/super-admin")
    @RequirePermission(PermissionCode.SUPER_ADMIN)
    public Map<String, Object> superAdminEndpoint() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "超级管理员专用接口");
        result.put("userId", AuthContext.getCurrentUserId());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
