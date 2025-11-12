package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户权限表
 * </p>
 * 
 * @author sara
 * @since 2024-09-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_permissions")
public class UserPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 商品编码
     */
    @TableField("good_code")
    private String goodCode;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 是否有访问权限
     */
    @TableField("has_access")
    private Boolean hasAccess = false;

    /**
     * 访问时间
     */
    @TableField("access_time")
    private LocalDateTime accessTime;

    /**
     * 到期时间
     */
    @TableField("expiry_time")
    private LocalDateTime expiryTime = LocalDateTime.now().plusYears(99);  // 默认99年后

    /**
     * 权限JSON对象
     */
    @TableField("permissions_json")
    private String permissionsJson;

    /**
     * 备注
     */
    @TableField("notes")
    private String notes;
}
