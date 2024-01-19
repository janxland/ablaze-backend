package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * <p>
 * 标签
 * </p>
 *
 * @author sara
 * @since 2021-09-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("collection")
public class Collection implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 合集名称
     */
    @TableField("collection_name")
    private String collectionName;

    /**
     * 合集封面
     */
    @TableField("collection_poster")
    private String collectionPoster;

    /**
     * 合集封面
     */
    @TableField("collection_items")
    private String collectionItems;

    /**
     * 合集描述
     */
    @TableField("collection_description")
    private String collectionDescription;

    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    private Integer countOfLabel;
}
