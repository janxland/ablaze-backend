package com.ld.poetry.controller;

import com.alibaba.fastjson.JSON;
import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.service.UserArticleAuthService;
import com.ld.poetry.utils.PaymentNotifyDTO;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.config.PoetryResult;

import org.springframework.validation.annotation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@Tag(name = "UserArticleAuth", description = "文章访问授权")
@RequestMapping("/userArticleAuth")
public class UserArticleAuthController {

    @Resource
    private UserArticleAuthService userArticleAuthService;

    /**
     * 查询订单状态
     */
    @Operation(summary = "查询订单状态")
    @PostMapping("/query")
    public Object queryOrder(
        @Validated @RequestBody PaymentNotifyDTO  paymentNotifyDTO
         ) {
        if(PoetryUtil.getUserId() == null) {
            return PoetryResult.fail("未登录");
        }
        if(paymentNotifyDTO.getProductCode() == null) {
            return PoetryResult.fail("ProductCode 不能为空");
        }
        return JSON.parseObject(userArticleAuthService.queryOrderStatus(paymentNotifyDTO));

    }

    /**
     * 创建付费解锁订单
     */
    @Operation(summary = "创建付费解锁订单")
    @PostMapping("/create")
    public Object createUserArticleAuthOther(
        @Validated @RequestBody PaymentNotifyDTO  paymentNotifyDTO ) {
        if(paymentNotifyDTO.getProductCode() == null) {
            return PoetryResult.fail("ProductCode 不能为空");
        }
        if(PoetryUtil.getUserId() == null) {
            return PoetryResult.fail("未登录");
        }
        return JSON.parseObject(userArticleAuthService.createOrder(paymentNotifyDTO));

    }

    /**
     * 查询当前用户对某文章的访问授权
     */
    @Operation(summary = "查询当前用户对某文章的访问授权")
    @GetMapping("/get")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public Object getUserArticleAuth(@RequestParam Integer userId,
                                              @RequestParam Integer articleId) {
        // 只允许查自己的授权，防止越权遍历
        Integer current = PoetryUtil.getUserId();
        if (current == null || !current.equals(userId)) {
            return PoetryResult.fail("仅允许查询本人授权");
        }
        return userArticleAuthService.findByUserAndArticle(userId, articleId);
    }

    /**
     * 创建或更新授权（管理员维护用）
     */
    @Operation(summary = "创建或更新授权（管理员维护用）")
    @PostMapping("/createOrUpdate")
    @RequirePermission(PermissionCode.USER_ADMIN)
    public UserArticleAuth createOrUpdate(@RequestBody UserArticleAuth userArticleAuth) {
        return userArticleAuthService.createOrUpdate(userArticleAuth);
    }

}
