package com.ld.poetry.controller;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.auth.AuthContext;
import com.ld.poetry.entity.User;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.auth.SimpleAuthHelper;
import com.ld.poetry.service.UserCacheService;
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
    private UserCacheService userCacheService;

    @Autowired
    private SimpleAuthHelper simpleAuthHelper;

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
            UserCacheService.CachedUserInfo cachedUser = userCacheService.getOrCreateUserMapping(token);
            if (cachedUser.exists) {
                Map<String, Object> mappingInfo = new HashMap<>();
                mappingInfo.put("localUserId", cachedUser.localUserId);
                mappingInfo.put("localUsername", cachedUser.username);
                mappingInfo.put("localEmail", cachedUser.email);
                mappingInfo.put("localUserType", cachedUser.userType);
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
     * Cookie Token测试接口 - 测试从Cookie中提取sso_access_token
     */
    @GetMapping("/cookie-test")
    public Map<String, Object> cookieTokenTest() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 使用SimpleAuthHelper直接从请求中解析用户信息（包括Cookie）
            AuthContext.UserInfo userInfo = simpleAuthHelper.parseUserFromRequest();
            
            if (userInfo != null) {
                result.put("success", true);
                result.put("source", "Cookie中的sso_access_token");
                result.put("userId", userInfo.getUserId());
                result.put("username", userInfo.getUsername());
                result.put("email", userInfo.getEmail());
                result.put("userType", userInfo.getUserType());
                result.put("isAdmin", userInfo.isAdmin());
                result.put("isSuperAdmin", userInfo.isSuperAdmin());
                result.put("message", "从Cookie成功解析用户信息");
                
                // 权限测试
                Map<String, Object> permissions = new HashMap<>();
                permissions.put("LOGIN_REQUIRED", true);
                permissions.put("USER_ADMIN", userInfo.isAdmin());
                permissions.put("SUPER_ADMIN", userInfo.isSuperAdmin());
                result.put("permissions", permissions);
                
            } else {
                result.put("success", false);
                result.put("message", "未找到有效的token（Authorization header或Cookie中的sso_access_token）");
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "Cookie token解析失败: " + e.getMessage());
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
