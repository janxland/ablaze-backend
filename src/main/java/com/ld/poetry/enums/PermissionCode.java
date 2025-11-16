package com.ld.poetry.enums;

/**
 * 权限码枚举
 * 统一管理所有权限代码，方便维护和使用
 */
public enum PermissionCode {
    
    // ==================== 基础权限 ====================
    /** 公开访问 */
    PUBLIC("PUBLIC", "公开访问"),
    /** 需要登录 */
    LOGIN_REQUIRED("LOGIN_REQUIRED", "需要登录"),
    
    // ==================== 用户权限 ====================
    /** 普通用户权限 */
    USER_NORMAL("USER_NORMAL", "普通用户"),
    /** 管理员权限 */
    USER_ADMIN("USER_ADMIN", "管理员"),
    /** 超级管理员权限 */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员"),
    
    // ==================== 内容管理权限 ====================
    /** 查看文章 */
    ARTICLE_VIEW("ARTICLE_VIEW", "查看文章"),
    /** 创建文章 */
    ARTICLE_CREATE("ARTICLE_CREATE", "创建文章"),
    /** 编辑文章 */
    ARTICLE_EDIT("ARTICLE_EDIT", "编辑文章"),
    /** 删除文章 */
    ARTICLE_DELETE("ARTICLE_DELETE", "删除文章"),
    /** 审核文章 */
    ARTICLE_AUDIT("ARTICLE_AUDIT", "审核文章"),
    
    // ==================== 评论权限 ====================
    /** 查看评论 */
    COMMENT_VIEW("COMMENT_VIEW", "查看评论"),
    /** 发表评论 */
    COMMENT_CREATE("COMMENT_CREATE", "发表评论"),
    /** 删除评论 */
    COMMENT_DELETE("COMMENT_DELETE", "删除评论"),
    
    // ==================== 系统管理权限 ====================
    /** 用户管理 */
    SYSTEM_USER_MANAGE("SYSTEM_USER_MANAGE", "用户管理"),
    /** 系统配置 */
    SYSTEM_CONFIG("SYSTEM_CONFIG", "系统配置"),
    /** 数据统计 */
    SYSTEM_STATISTICS("SYSTEM_STATISTICS", "数据统计"),
    
    // ==================== 文件上传权限 ====================
    /** 文件上传 */
    FILE_UPLOAD("FILE_UPLOAD", "文件上传"),
    /** 获取上传Token */
    FILE_UPLOAD_TOKEN("FILE_UPLOAD_TOKEN", "获取上传Token");
    
    private final String code;
    private final String description;
    
    PermissionCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 根据代码获取权限枚举
     */
    public static PermissionCode fromCode(String code) {
        for (PermissionCode permission : values()) {
            if (permission.code.equals(code)) {
                return permission;
            }
        }
        return null;
    }
}
