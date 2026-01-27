package com.ld.poetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("orders")
public class Order {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_id")
    private String orderId = generateUniqueId(32);

    @TableField("amount")
    private BigDecimal amount = BigDecimal.ONE;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("payment_type")
    private String paymentType;

    @TableField("transaction_id")
    private String transactionId;

    @TableField("transaction_info")
    private String transactionInfo;

    @TableField("product_id")
    private String productId;

    @TableField("system_id")
    private String systemId;

    @TableField("create_time")
    private Long createTime = Instant.now().toEpochMilli();

    @TableField("update_time")
    private Long updateTime = Instant.now().toEpochMilli();

    // Generate a unique identifier for the order
    private String generateUniqueId(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return result.toString();
    }

    // Getters and Setters for all fields

}
