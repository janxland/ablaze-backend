package com.ld.poetry.controller;

import org.springframework.web.bind.annotation.*;

import com.ld.poetry.config.LoginCheck;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.utils.*;

/**
 * 七牛云
 */
@RestController
@RequestMapping("/qiniu")
public class QiniuController {

    /**
     * 获取覆盖凭证
     */
    @GetMapping("/getUpToken")
    @LoginCheck
    public PoetryResult<String> getUpToken(@RequestParam(value = "key", required = false) String key) {
        PoetryUtil.checkEmail();
        return PoetryResult.success(QiniuUtil.getToken(key));
    }
}
