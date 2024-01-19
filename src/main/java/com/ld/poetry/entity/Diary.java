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
@TableName("diary")
public class Diary implements Serializable {

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
    @TableField("article_items")
    private String articleItems;

    /**
     * 日记标题
     */
    @TableField("article_title")
    private String articleTitle;

    /**
     * 当日心情
     */
    @TableField("article_emotion")
    private String articleEmotion;

    /**
     * 日记内容
     */
    @TableField("article_content")
    private String articleContent;

    /**
     * 地点
     */
    @TableField("article_address")
    private String articleAddress;

    /**
     * 设备
     */
    @TableField("article_device")
    private String articleDevice;

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
