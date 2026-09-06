package com.ld.poetry.controller;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.utils.QiniuUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 七牛云 - 使用新的权限系统
 */
@RestController
@Tag(name = "Qiniu", description = "对象存储上传")
@RequestMapping("/qiniu")
public class QiniuController {

    /**
     * 获取上传凭证
     * 需要登录且绑定邮箱
     */
    @Operation(summary = "需要登录且绑定邮箱")
    @GetMapping("/getUpToken")
    @RequirePermission(PermissionCode.FILE_UPLOAD_TOKEN)
    public PoetryResult<String> getUpToken(@RequestParam(value = "key", required = false) String key) {
        return PoetryResult.success(QiniuUtil.getToken(key));
    }
}
