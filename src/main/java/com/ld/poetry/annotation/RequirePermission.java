package com.ld.poetry.annotation;

import com.ld.poetry.enums.PermissionCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限守卫注解
 * 简单、直观的权限控制
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * 需要的权限码
     */
    PermissionCode value() default PermissionCode.LOGIN_REQUIRED;
    
    /**
     * 权限描述（可选）
     */
    String description() default "";
}
