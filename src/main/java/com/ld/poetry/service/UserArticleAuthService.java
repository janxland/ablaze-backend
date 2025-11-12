package com.ld.poetry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.entity.Order;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.utils.PaymentNotifyDTO;

public interface UserArticleAuthService extends IService<UserArticleAuth> {
    String queryOrderStatus(PaymentNotifyDTO paymentNotifyDTO) ;
    String createOrder(PaymentNotifyDTO paymentNotifyDTO);
    /**
     * 根据 userId 和 articleId 查询
     */
    UserArticleAuth findByUserAndArticle(Integer userId, Integer articleId);

    /**
     * 创建或更新
     * @param userArticleAuth 传入包含 userId, articleId, vip1, pay, reply 等信息
     * @return 创建/更新后的实体
     */
    UserArticleAuth createOrUpdate(UserArticleAuth userArticleAuth);

}
