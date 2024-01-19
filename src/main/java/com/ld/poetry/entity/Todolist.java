package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author sara
 * @since 2023-07-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("todolist")
public class Todolist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("dependency_id")
    private Integer dependencyId;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("status")
    private String status;

    @TableField("start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @TableField("end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @TableField("urgency")
    private Integer urgency;

    @TableField("executor")
    private String executor;

    @TableField("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @TableField("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;


}
