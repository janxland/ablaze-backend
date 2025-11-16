package com.ld.poetry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT密钥，与Node.js鉴权中心保持一致
     */
    private String secret = "J$A@N#X$L%A&N*D";
    
    /**
     * Token过期时间（秒），默认24小时
     */
    private Long expiration = 86400L;
    
    /**
     * 是否启用JWT认证
     */
    private Boolean enabled = true;
}
