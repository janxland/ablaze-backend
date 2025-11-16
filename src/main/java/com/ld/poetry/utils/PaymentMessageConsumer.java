package com.ld.poetry.utils;

import org.springframework.stereotype.Component;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import com.rabbitmq.client.Channel;
import org.springframework.messaging.handler.annotation.Payload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;

import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.service.UserArticleAuthService;

@Component
@RequiredArgsConstructor
public class PaymentMessageConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentMessageConsumer.class);
    
    private final RabbitTemplate rabbitTemplate;  // 注入 RabbitTemplate
    private final UserArticleAuthService userArticleAuthService;

    // 监听指定队列（${spring.rabbitmq.queue.payment}）
    @RabbitListener(queuesToDeclare = @Queue(name = "${spring.rabbitmq.queue.payment}"))
    public void handlePaymentMessage(
            @Payload String payload,
            Channel channel,
            Message message) throws IOException {
    
        try {
            System.out.println("Received payment message: " + payload);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(payload, Map.class);
            
            // 判断消息内容，只有成功的支付消息才会进一步处理
            if(payloadMap.get("action").toString().equals("SUCCESS")) {
                PaymentNotifyDTO orderMap = convertToDTO(payloadMap);
                System.out.println(orderMap.toString());
                
                // 处理订单信息并更新支付状态
                if(orderMap.getUserId() != null && orderMap.getProductId() != null && orderMap.getOrderStatus() == PaymentNotifyDTO.PaymentStatus.SUCCESS) {
                    setUserArticlePay(convertToDTO(payloadMap));
                }
            }
            
            // 确认消息处理成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // 异常处理，记录日志，并将消息重新排队
            logger.error("处理支付消息失败，消息进入重试队列", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    // 发送消息到指定队列
    public void sendMessageToQueue(String message) {
        try {
            // 发送到同一队列 ${spring.rabbitmq.queue.payment}
            rabbitTemplate.convertAndSend("${spring.rabbitmq.queue.payment}", message);
            logger.info("消息已发送到队列[{}]: {}", "${spring.rabbitmq.queue.payment}", message);
        } catch (Exception e) {
            logger.error("发送消息到队列失败", e);
        }
    }

    // 将 payload 转换为 DTO 对象
    private PaymentNotifyDTO convertToDTO(Map<String, Object> payload) {
        // 使用 Jackson 将 Map 转换为 DTO 对象
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(payload, PaymentNotifyDTO.class);
    }

    // 更新用户文章支付状态
    private void setUserArticlePay(PaymentNotifyDTO notify) {
        try {
            UserArticleAuth auth = new UserArticleAuth();
            auth.setUserId(Integer.valueOf(notify.getUserId()));
            auth.setArticleId(Integer.valueOf(notify.getProductId()));
            auth.setPay(1); // 标记为已支付
            
            // 更新数据库
            userArticleAuthService.createOrUpdate(auth);
            logger.info("用户[{}]文章[{}]权限已更新", notify.getUserId(), notify.getProductId());
        } catch (DataAccessException e) {
            logger.error("数据库更新失败", e);
            throw new RuntimeException("DB操作异常", e);
        }
    }
}
