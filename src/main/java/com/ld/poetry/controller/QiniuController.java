package com.ld.poetry.controller;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.utils.QiniuUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 七牛云 - 使用新的权限系统
 */
@RestController
@RequestMapping("/qiniu")
public class QiniuController {

    /**
     * 获取上传凭证
     * 需要登录且绑定邮箱
     */
    @GetMapping("/getUpToken")
    @RequirePermission(PermissionCode.FILE_UPLOAD_TOKEN)
    public PoetryResult<String> getUpToken(@RequestParam(value = "key", required = false) String key) {
        return PoetryResult.success(QiniuUtil.getToken(key));
    }
}
