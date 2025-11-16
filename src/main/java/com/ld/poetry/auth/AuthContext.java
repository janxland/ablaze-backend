package com.ld.poetry.auth;

/**
 * 认证上下文 - 简单扁平化的用户信息存储
 */
public class AuthContext {
    
    private static final ThreadLocal<UserInfo> CURRENT_USER = new ThreadLocal<>();
    
    /**
     * 用户信息
     */
    public static class UserInfo {
        private Integer userId;
        private String username;
        private String email;
        private String userType;
        
        public UserInfo(Integer userId, String username, String email, String userType) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.userType = userType;
        }
        
        // Getters
        public Integer getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getUserType() { return userType; }
        
        public boolean isAdmin() {
            return "ADMIN".equals(userType) || "SUPER_ADMIN".equals(userType);
        }
        
        public boolean isSuperAdmin() {
            return "SUPER_ADMIN".equals(userType);
        }
    }
    
    /**
     * 设置当前用户
     */
    public static void setCurrentUser(Integer userId, String username, String email, String userType) {
        CURRENT_USER.set(new UserInfo(userId, username, email, userType));
    }
    
    /**
     * 获取当前用户
     */
    public static UserInfo getCurrentUser() {
        return CURRENT_USER.get();
    }
    
    /**
     * 获取当前用户ID
     */
    public static Integer getCurrentUserId() {
        UserInfo user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
    
    /**
     * 获取当前用户邮箱
     */
    public static String getCurrentEmail() {
        UserInfo user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }
    
    /**
     * 检查是否已登录
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
    
    /**
     * 检查是否是管理员
     */
    public static boolean isAdmin() {
        UserInfo user = getCurrentUser();
        return user != null && user.isAdmin();
    }
    
    /**
     * 检查是否是超级管理员
     */
    public static boolean isSuperAdmin() {
        UserInfo user = getCurrentUser();
        return user != null && user.isSuperAdmin();
    }
    
    /**
     * 清除当前用户信息
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
