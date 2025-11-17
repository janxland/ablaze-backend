package com.ld.poetry.auth;

import com.ld.poetry.service.UserCacheService;
import com.ld.poetry.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 简单的认证助手 - 不依赖过滤器
 */
@Slf4j
@Component
public class SimpleAuthHelper {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserCacheService userCacheService;

    /**
     * 直接从请求中解析用户信息
     */
    public AuthContext.UserInfo parseUserFromRequest() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request == null) {
                log.warn("无法获取当前请求");
                return null;
            }

            String token = extractToken(request);
            if (token == null) {
                log.warn("未找到JWT token");
                return null;
            }

            return parseUserFromToken(token);
        } catch (Exception e) {
            log.error("解析用户信息失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从token解析用户信息 - 支持用户映射
     */
    public AuthContext.UserInfo parseUserFromToken(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                log.warn("JWT token验证失败");
                return null;
            }

            // 使用Redis缓存的用户映射服务
            UserCacheService.CachedUserInfo cachedUser = userCacheService.getOrCreateUserMapping(token);
            
            if (!cachedUser.exists) {
                log.warn("用户映射失败或用户不存在");
                return null;
            }

            log.info("用户认证成功 - 本地ID: {}, 用户名: {}, 用户类型: {}", 
                    cachedUser.localUserId, cachedUser.username, cachedUser.userType);

            return new AuthContext.UserInfo(cachedUser.localUserId, cachedUser.username, 
                    cachedUser.email, cachedUser.userType);
        } catch (Exception e) {
            log.error("解析JWT token失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 提取JWT token - 支持Authorization header和Cookie
     */
    private String extractToken(HttpServletRequest request) {
        // 1. 优先从Authorization header获取
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader)) {
            // 支持带Bearer前缀和不带前缀的token
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            } else {
                return authHeader;
            }
        }
        
        // 2. 从Cookie中获取sso_access_token（适用于*.roginx.ink域名）
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        log.info("检查Cookie - cookies数量: {}", cookies != null ? cookies.length : 0);
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                log.info("Cookie: {} = {}", cookie.getName(), cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                if ("sso_access_token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.info("从Cookie获取到sso_access_token: {}", token.substring(0, Math.min(20, token.length())) + "...");
                        return token;
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

}
