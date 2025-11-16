package com.ld.poetry.utils;

import com.ld.poetry.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 * 与Node.js鉴权中心共享相同的JWT_SECRET
 */
@Slf4j
@Component
public class JwtUtil {
    
    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // 使用与Node.js相同的密钥，如果密钥太短则进行安全扩展
        String secret = jwtProperties.getSecret();
        if (secret.length() < 32) {
            // 对于短密钥，使用确定性方法扩展到256位
            secret = expandSecretKey(secret);
            log.info("JWT密钥已扩展到256位以满足安全要求");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将短密钥扩展到256位（32字符）
     * 使用确定性方法，确保每次扩展结果一致
     */
    private String expandSecretKey(String originalSecret) {
        StringBuilder expanded = new StringBuilder(originalSecret);
        
        // 重复原始密钥直到达到32字符
        while (expanded.length() < 32) {
            expanded.append(originalSecret);
        }
        
        // 截取到32字符
        return expanded.substring(0, 32);
    }

    /**
     * 解析JWT token
     * @param token JWT token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token已过期: {}", e.getMessage());
            throw new RuntimeException("Token已过期");
        } catch (UnsupportedJwtException e) {
            log.warn("不支持的JWT token: {}", e.getMessage());
            throw new RuntimeException("不支持的Token格式");
        } catch (MalformedJwtException e) {
            log.warn("JWT token格式错误: {}", e.getMessage());
            throw new RuntimeException("Token格式错误");
        } catch (SecurityException e) {
            log.warn("JWT token签名验证失败: {}", e.getMessage());
            throw new RuntimeException("Token签名验证失败");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token参数错误: {}", e.getMessage());
            throw new RuntimeException("Token参数错误");
        }
    }

    /**
     * 验证token是否有效
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从token中获取用户ID
     * @param token JWT token
     * @return 用户ID
     */
    public Integer getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        // 兼容不同的用户ID字段名
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("id");
        }
        if (userId == null) {
            userId = claims.getSubject();
        }
        
        if (userId instanceof Number) {
            return ((Number) userId).intValue();
        } else if (userId instanceof String) {
            return Integer.parseInt((String) userId);
        }
        
        throw new RuntimeException("无法从token中获取用户ID");
    }

    /**
     * 从token中获取用户名
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        String username = (String) claims.get("username");
        if (username == null) {
            username = (String) claims.get("name");
        }
        return username;
    }

    /**
     * 从token中获取邮箱
     * @param token JWT token
     * @return 邮箱
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        String email = (String) claims.get("email");
        
        // 如果认证中心token没有email，根据用户名生成默认邮箱
        if (email == null) {
            String username = (String) claims.get("username");
            if (username != null) {
                // 为认证中心用户生成默认邮箱
                email = username + "@auth-center.local";
            }
        }
        
        return email;
    }

    /**
     * 检查token是否即将过期（30分钟内）
     * @param token JWT token
     * @return 是否即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            Date now = new Date();
            long timeUntilExpiration = expiration.getTime() - now.getTime();
            // 如果30分钟内过期，返回true
            return timeUntilExpiration < 30 * 60 * 1000;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 生成JWT token
     * @param userId 用户ID
     * @param username 用户名
     * @param email 邮箱
     * @param userType 用户类型（NORMAL/ADMIN）
     * @return JWT token
     */
    public String generateToken(Integer userId, String username, String email, String userType) {
        return generateToken(userId, username, email, userType, null);
    }

    /**
     * 生成JWT token（完整版）
     * @param userId 本地用户ID
     * @param username 用户名
     * @param email 邮箱
     * @param userType 用户类型
     * @param authCenterUserId 认证中心用户ID（可选）
     * @return JWT token
     */
    public String generateToken(Integer userId, String username, String email, String userType, Integer authCenterUserId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);

        // 智能判断用户类型
        String finalUserType = userType;
        if (userId != null && (userId.equals(1) || userId.equals(983341575))) {
            finalUserType = "SUPER_ADMIN";
        }

        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("username", username)
                .claim("email", email)
                .claim("userType", finalUserType)
                .claim("authSource", "local-project")  // 标识为本项目token
                .setIssuedAt(now)
                .setExpiration(expiration);

        // 如果有认证中心用户ID，添加映射信息
        if (authCenterUserId != null) {
            builder.claim("authCenterUserId", authCenterUserId);
        }

        return builder.signWith(secretKey, SignatureAlgorithm.HS256).compact();
    }

    /**
     * 从token中获取用户类型
     * @param token JWT token
     * @return 用户类型
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        
        // 优先使用本项目的userType字段
        String userType = (String) claims.get("userType");
        if (userType != null) {
            return userType;
        }
        
        // 兼容认证中心的currentRoleCode字段
        String currentRoleCode = (String) claims.get("currentRoleCode");
        if (currentRoleCode != null) {
            return currentRoleCode;
        }
        
        // 兼容认证中心的roleCodes数组，取第一个
        Object roleCodes = claims.get("roleCodes");
        if (roleCodes instanceof java.util.List) {
            java.util.List<?> roleList = (java.util.List<?>) roleCodes;
            if (!roleList.isEmpty()) {
                return String.valueOf(roleList.get(0));
            }
        }
        
        return null;
    }

    /**
     * 从token中获取认证主体（项目来源）
     * @param token JWT token
     * @return 认证主体，默认为"auth-center"
     */
    public String getAuthSourceFromToken(String token) {
        Claims claims = parseToken(token);
        String authSource = (String) claims.get("authSource");
        return authSource != null ? authSource : "auth-center";
    }

    /**
     * 从token中获取认证中心用户ID
     * @param token JWT token
     * @return 认证中心用户ID
     */
    public Integer getAuthCenterUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        
        // 如果是认证中心token，userId就是认证中心ID
        String authSource = getAuthSourceFromToken(token);
        if ("auth-center".equals(authSource)) {
            return getUserIdFromToken(token);
        }
        
        // 如果是本项目token，查找authCenterUserId字段
        Object authCenterUserId = claims.get("authCenterUserId");
        if (authCenterUserId instanceof Number) {
            return ((Number) authCenterUserId).intValue();
        } else if (authCenterUserId instanceof String) {
            return Integer.parseInt((String) authCenterUserId);
        }
        
        return null;
    }

}
