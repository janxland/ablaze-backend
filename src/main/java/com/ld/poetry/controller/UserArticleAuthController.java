package com.ld.poetry.controller;

import com.alibaba.fastjson.JSON;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Order;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.service.UserArticleAuthService;
import com.ld.poetry.utils.PaymentNotifyDTO;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.vo.ArticleVO;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/userArticleAuth")
public class UserArticleAuthController {

    @Resource
    private UserArticleAuthService userArticleAuthService;
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
     * 通过 userId + articleId 查询
     */
    @PostMapping("/create")
    public Object createUserArticleAuthOther(
        @Validated @RequestBody PaymentNotifyDTO  paymentNotifyDTO ) {
        System.out.println("paymentNotifyDTO = " + paymentNotifyDTO);
        if(paymentNotifyDTO.getProductCode() == null) {
            return PoetryResult.fail("ProductCode 不能为空");
        }
        if(PoetryUtil.getUserId() == null) {
            return PoetryResult.fail("未登录");
        }
        return JSON.parseObject(userArticleAuthService.createOrder(paymentNotifyDTO));
        
    }
    /**
     * 通过 userId + articleId 查询
     */
    @GetMapping("/get")
    public UserArticleAuth getUserArticleAuth(@RequestParam Integer userId,
                                              @RequestParam Integer articleId) {
        return userArticleAuthService.findByUserAndArticle(userId, articleId);
    }

    /**
     * 创建或更新 (同一个接口)
     */
    @PostMapping("/createOrUpdate")
    public UserArticleAuth createOrUpdate(@RequestBody UserArticleAuth userArticleAuth) {
        return userArticleAuthService.createOrUpdate(userArticleAuth);
    }

}
