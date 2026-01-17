package com.ld.poetry.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ld.poetry.utils.JsonUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.config.ApplicationProperties;
import com.ld.poetry.dao.UserArticleAuthMapper;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.service.UserArticleAuthService;
import com.ld.poetry.utils.PaymentNotifyDTO;
import com.ld.poetry.utils.PoetryUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class UserArticleAuthServiceImpl
        extends ServiceImpl<UserArticleAuthMapper, UserArticleAuth>
        implements UserArticleAuthService {

    @Autowired
    private ApplicationProperties applicationProperties;
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Override
    public String createOrder(PaymentNotifyDTO paymentNotifyDTO) {
        String url = applicationProperties.getPaymentApiUrl();
        if (url == null || url.isEmpty()) {
            
        }
        log.debug("使用支付API URL: {}", url);
        String params = "systemId=1&userId=" + PoetryUtil.getUserId()  + 
                        "&productCode=" + paymentNotifyDTO.getProductCode() +
                        "&amount=" + 5 +
                        "&paymentMethod=" + paymentNotifyDTO.getPaymentMethod();

        try {
            URL urlObj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = params.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (IOException e) {
            log.error("创建订单失败，服务器请求异常", e);
            throw new RuntimeException("Order creation failed, server request exception", e);
        }
    }
    
   /**
     * 根据订单号查询订单状态
     * @param orderId 订单号
     * @return 返回完整的响应
     */
    @Override
    public String queryOrderStatus(PaymentNotifyDTO dto){
        try {
            dto.setUserId(String.valueOf(PoetryUtil.getUserId()));
            Map<String, Object> params = OBJECT_MAPPER.convertValue(dto, 
                OBJECT_MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            StringJoiner query = new StringJoiner("&");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    query.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + 
                            URLEncoder.encode(value.toString(), "UTF-8"));
                }
            }
            log.debug("查询订单参数: {}", query);
            HttpURLConnection conn = (HttpURLConnection) new URL(applicationProperties.getPaymentStatusApiUrl() + "?" + query).openConnection();
            try (InputStream is = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                
                String responseBody = br.lines().collect(Collectors.joining());
                JsonNode res = OBJECT_MAPPER.readTree(responseBody);
                JsonNode dataNode = res.get("data");
                if (dataNode != null && "SUCCESS".equals(dataNode.get("orderStatus").asText())) {
                    UserArticleAuth auth = new UserArticleAuth();
                    auth.setUserId(PoetryUtil.getUserId());
                    auth.setArticleId(Integer.valueOf(dto.getProductCode()));
                    auth.setPay(1); // 标记为已支付
                    this.createOrUpdate(auth);
                    log.info("订单支付成功，用户ID: {}, 文章ID: {}", PoetryUtil.getUserId(), dto.getProductCode());
                }
                return responseBody;
            } catch (Exception e) {
                log.error("查询订单状态异常", e);
                return e.getMessage();
            }
        } catch (Exception e) {
            log.error("查询订单状态失败", e);
            return e.getMessage();
        }
    }

    /**
     * 根据 userId, articleId 查询
     */
    @Override
    public UserArticleAuth findByUserAndArticle(Integer userId, Integer articleId) {
            QueryWrapper<UserArticleAuth> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda()
                    .eq(UserArticleAuth::getUserId, userId)
                    .eq(UserArticleAuth::getArticleId, articleId);
        // getOne: 如果可能返回多条，要注意加适当限制，一般这种 user_id + article_id 是唯一约束才可用 getOne
        return this.getOne(queryWrapper);
    }

    /**
     * 创建或更新
     *  - 根据 userId, articleId 判断是否已存在
     *  - 如果存在则更新字段
     *  - 如果不存在则插入新纪录
     */
    @Override
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public UserArticleAuth createOrUpdate(UserArticleAuth userArticleAuth) {

        // 1. 先查是否已有记录
        UserArticleAuth exist = findByUserAndArticle(
                userArticleAuth.getUserId(),
                userArticleAuth.getArticleId()
        );

        // 2. 如果已存在 -> 更新
        if (exist != null) {
            exist.setVip1(userArticleAuth.getVip1());
            exist.setPay(userArticleAuth.getPay());
            exist.setReply(userArticleAuth.getReply());

            // 调用 mybatis-plus 的 updateById
            this.updateById(exist);
            return exist;
        }

        // 3. 如果不存在 -> 插入
        this.save(userArticleAuth);
        return userArticleAuth;
    }

}
