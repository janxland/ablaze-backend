package com.ld.poetry.utils;

import com.ld.poetry.auth.AuthContext;

/**
 * 简化的认证工具类
 * 委托给AuthContext处理，保持向后兼容
 */
public class AuthUtil {

    /**
     * 获取当前用户ID
     */
    public static Integer getCurrentLocalUserId() {
        return AuthContext.getCurrentUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        AuthContext.UserInfo user = AuthContext.getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentEmail() {
        return AuthContext.getCurrentEmail();
    }

    /**
     * 获取当前用户类型
     */
    public static String getCurrentUserType() {
        AuthContext.UserInfo user = AuthContext.getCurrentUser();
        return user != null ? user.getUserType() : null;
    }

    /**
     * 检查是否已认证
     */
    public static boolean isAuthenticated() {
        return AuthContext.isLoggedIn();
    }

    /**
     * 要求用户已认证
     */
    public static Integer requireAuthentication() {
        Integer userId = getCurrentLocalUserId();
        if (userId == null) {
            throw new RuntimeException("用户未认证");
        }
        return userId;
    }

    // 兼容旧代码的方法
    public static Integer getCurrentAuthCenterUserId() {
        return getCurrentLocalUserId();
    }
}
