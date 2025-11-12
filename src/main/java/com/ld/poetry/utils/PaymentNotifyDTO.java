package com.ld.poetry.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentNotifyDTO {
    // 必填字段
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("systemId")
    private String systemId;

    @JsonProperty("systemCode")
    private String systemCode;

    @JsonProperty("productDescription")
    private String productDescription;

    @JsonProperty("amount")
    private BigDecimal amount;

    // 支付状态（适配你的枚举）
    @JsonProperty("orderStatus")
    private PaymentStatus orderStatus;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // 可选扩展字段
    @JsonProperty("productId")
    private String productId;
    
    // 可选扩展字段
    @JsonProperty("productCode")
    private String productCode;

    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("customerEmail")
    private String customerEmail;

    // 支付状态枚举（根据实际需求调整）
    public enum PaymentStatus {
        @JsonProperty("SUCCESS") SUCCESS,
        @JsonProperty("FAILED") FAILED,
        @JsonProperty("PENDING") PENDING
    }
}