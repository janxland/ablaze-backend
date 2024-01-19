package com.ld.poetry.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

@Data
public class BaseRequestVO extends Page {

    private Integer source;

    private Integer floorCommentId;

    private String keywords;

    private String desc;
    // 是否推荐[0:否，1:是]
    private Boolean recommendStatus;

    private Integer sortId;

    private Integer labelId;

    private Boolean userStatus;

    private Integer userType;

    private Integer userId;

    private String resourceType;

    private Boolean resourceStatus;
}
