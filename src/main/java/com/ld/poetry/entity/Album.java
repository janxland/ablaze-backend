package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 文章表
 * </p>
 *
 * @author sara
 * @since 2023-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("album")
public class Album implements Serializable {

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
     * 分类ID
     */
    @TableField("sort_id")
    private Integer sortId;

    /**
     * 标签ID
     */
    @TableField("label_id")
    private Integer labelId;

    /**
     * 封面
     */
    @TableField("album_cover")
    private String albumCover;

    /**
     * 博文标题
     */
    @TableField("album_title")
    private String albumTitle;

    /**
     * 影集
     */
    @TableField("album_item")
    private String albumItem;

    /**
     * 影集介绍
     */
    @TableField("album_desc")
    private String albumDesc;

    /**
     * 地点
     */
    @TableField("album_address")
    private String albumAddress;

    /**
     * 设备
     */
    @TableField("album_device")
    private String albumDevice;

    /**
     * 关键词
     */
    @TableField("keywords")
    private String keywords;

    /**
     * 可见权限
     */
    @TableField("permission")
    private Integer permission;

    /**
     * 浏览量
     */
    @TableField("view_count")
    private Integer viewCount;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 是否可见[0:否，1:是]
     */
    @TableField("view_status")
    private Boolean viewStatus;

    /**
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 是否推荐[0:否，1:是]
     */
    @TableField("recommend_status")
    private Boolean recommendStatus;

    /**
     * 是否启用评论[0:否，1:是]
     */
    @TableField("comment_status")
    private Boolean commentStatus;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 最终修改时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 最终修改人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 是否启用[0:未删除，1:已删除]
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;


}
