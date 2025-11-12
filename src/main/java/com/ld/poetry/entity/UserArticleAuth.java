package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-文章鉴权表
 */
@Data
@TableName(value = "user_article_auth") // 指定数据库表名
public class UserArticleAuth {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id; // 主键

    @TableField(value = "user_id")
    private Integer userId;  // user表的ID

    @TableField(value = "article_id")
    private Integer articleId; // article表的ID

    @TableField(value = "vip1")
    private Integer vip1;  // 0=无权限,1=有权限

    @TableField(value = "pay")
    private Integer pay;   // 0=未付费,1=已付费

    @TableField(value = "reply")
    private Integer reply; // 0=回复权限,1=
}
