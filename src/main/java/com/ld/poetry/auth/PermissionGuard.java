package com.ld.poetry.auth;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.enums.PermissionCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限守卫 - 简单扁平化的权限检查
 */
@Slf4j
@Aspect
@Component
public class PermissionGuard {

    @Autowired
    private SimpleAuthHelper authHelper;

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        PermissionCode requiredPermission = requirePermission.value();
        
        // 公开接口直接放行
        if (requiredPermission == PermissionCode.PUBLIC) {
            return joinPoint.proceed();
        }
        
        // 直接从请求中解析用户信息，不依赖过滤器
        AuthContext.UserInfo currentUser = authHelper.parseUserFromRequest();
        
        // 同时设置到AuthContext中，供其他地方使用
        if (currentUser != null) {
            AuthContext.setCurrentUser(currentUser.getUserId(), currentUser.getUsername(), 
                    currentUser.getEmail(), currentUser.getUserType());
        }
        
        log.info("权限检查 - 需要权限: {}, 当前用户: {}, 用户类型: {}", 
                requiredPermission, 
                currentUser != null ? currentUser.getUserId() : "null",
                currentUser != null ? currentUser.getUserType() : "null");
        
        // 检查权限
        if (!hasPermission(requiredPermission, currentUser)) {
            String message = getPermissionErrorMessage(requiredPermission);
            log.warn("权限检查失败: {}, 当前用户: {}", message, currentUser != null ? currentUser.getUserId() : "null");
            throw new RuntimeException(message);
        }
        
        log.info("权限检查通过");
        return joinPoint.proceed();
    }

    /**
     * 检查是否有指定权限
     */
    private boolean hasPermission(PermissionCode permission, AuthContext.UserInfo user) {
        
        switch (permission) {
            case PUBLIC:
                return true;
                
            case LOGIN_REQUIRED:
                return user != null;
                
            case USER_NORMAL:
                return user != null;
                
            case USER_ADMIN:
                return user != null && user.isAdmin();
                
            case SUPER_ADMIN:
                return user != null && user.isSuperAdmin();
                
            case FILE_UPLOAD_TOKEN:
                // 文件上传Token需要登录且有邮箱
                return user != null && user.getEmail() != null && !user.getEmail().isEmpty();
                
            case ARTICLE_CREATE:
            case ARTICLE_EDIT:
            case COMMENT_CREATE:
                return user != null; // 登录用户可以创建内容
                
            case ARTICLE_DELETE:
            case COMMENT_DELETE:
            case ARTICLE_AUDIT:
                return user != null && user.isAdmin(); // 管理员可以删除和审核
                
            case SYSTEM_USER_MANAGE:
            case SYSTEM_CONFIG:
            case SYSTEM_STATISTICS:
                return user != null && user.isSuperAdmin(); // 超级管理员系统权限
                
            default:
                return user != null; // 默认需要登录
        }
    }

    /**
     * 获取权限错误信息
     */
    private String getPermissionErrorMessage(PermissionCode permission) {
        switch (permission) {
            case LOGIN_REQUIRED:
                return "未登陆，请登陆后再进行操作！";
            case FILE_UPLOAD_TOKEN:
                return "请先绑定邮箱！";
            case USER_ADMIN:
                return "需要管理员权限！";
            case SUPER_ADMIN:
                return "需要超级管理员权限！";
            default:
                return "权限不足！";
        }
    }
}
