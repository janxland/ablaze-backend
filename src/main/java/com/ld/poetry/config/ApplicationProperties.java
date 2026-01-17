package com.ld.poetry.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 应用统一配置属性类
 * 统一管理所有配置属性，避免多个入口，减少开发者心智负担
 * 
 * 使用方式：
 * 1. 通过依赖注入使用：@Autowired private ApplicationProperties appProperties;
 * 2. 所有配置属性都通过此类访问，确保单例和统一入口
 * 
 * 注意：使用@Value是为了兼容现有配置项，未来可以逐步迁移到@ConfigurationProperties
 */
@Data
@Component
public class ApplicationProperties {

    /**
     * 用户验证码格式模板
     */
    @Value("${user.code.format}")
    private String userCodeFormat;

    /**
     * 支付订单生成API地址
     */
    @Value("${PAY_API_URL}")
    private String paymentApiUrl;

    /**
     * 支付状态查询API地址
     */
    @Value("${PAY_STATUS_API_URL}")
    private String paymentStatusApiUrl;

    /**
     * 支付验证密钥
     */
    @Value("${payment.secret-key}")
    private String paymentSecretKey;

    /**
     * 支付签名字段名
     */
    @Value("${payment.sign-field}")
    private String paymentSignField;

    /**
     * 邮件发件人（从spring.mail.username读取）
     */
    @Value("${spring.mail.username}")
    private String mailUsername;
}
