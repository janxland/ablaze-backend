package com.ld.poetry.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ld.poetry.entity.User;
import com.ld.poetry.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 扁平化用户缓存服务
 * 使用Redis缓存用户映射信息，避免频繁数据库查询
 */
@Slf4j
@Service
public class UserCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存key前缀
    private static final String USER_CACHE_PREFIX = "user:mapping:";
    // 缓存过期时间（2小时）
    private static final long CACHE_EXPIRE_HOURS = 2;

    /**
     * 缓存用户信息
     */
    public static class CachedUserInfo {
        public Integer localUserId;
        public String username;
        public String email;
        public String userType;
        public boolean exists;

        public CachedUserInfo() {}

        public CachedUserInfo(Integer localUserId, String username, String email, String userType, boolean exists) {
            this.localUserId = localUserId;
            this.username = username;
            this.email = email;
            this.userType = userType;
            this.exists = exists;
        }
    }

    /**
     * 获取或创建用户映射（带缓存）
     * @param token JWT token
     * @return 缓存的用户信息
     */
    public CachedUserInfo getOrCreateUserMapping(String token) {
        try {
            // 解析JWT基本信息
            String authSource = jwtUtil.getAuthSourceFromToken(token);
            Integer authCenterUserId = jwtUtil.getAuthCenterUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            String jwtUserType = jwtUtil.getUserTypeFromToken(token);

            // 生成缓存key
            String cacheKey = USER_CACHE_PREFIX + authSource + ":" + authCenterUserId;

            // 1. 先从缓存获取
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cachedData)) {
                CachedUserInfo cachedUser = objectMapper.readValue(cachedData, CachedUserInfo.class);
                log.debug("从缓存获取用户映射 - 缓存Key: {}, 本地ID: {}", cacheKey, cachedUser.localUserId);
                return cachedUser;
            }

            // 2. 缓存未命中，查询数据库
            log.info("缓存未命中，查询数据库 - 认证来源: {}, 认证中心ID: {}, 用户名: {}", 
                    authSource, authCenterUserId, username);

            CachedUserInfo userInfo;

            if ("auth-center".equals(authSource)) {
                // 认证中心用户处理
                userInfo = handleAuthCenterUser(authCenterUserId, username, email, jwtUserType);
            } else {
                // 本地用户直接查询
                User localUser = userService.getById(authCenterUserId);
                if (localUser != null) {
                    userInfo = new CachedUserInfo(
                        localUser.getId(),
                        localUser.getUsername(),
                        localUser.getEmail(),
                        convertUserType(localUser.getUserType()),
                        true
                    );
                } else {
                    userInfo = new CachedUserInfo(null, null, null, null, false);
                }
            }

            // 3. 缓存结果
            String jsonData = objectMapper.writeValueAsString(userInfo);
            redisTemplate.opsForValue().set(cacheKey, jsonData, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("用户映射已缓存 - 缓存Key: {}, 过期时间: {}小时", cacheKey, CACHE_EXPIRE_HOURS);

            return userInfo;

        } catch (Exception e) {
            log.error("用户映射缓存处理失败: {}", e.getMessage(), e);
            return new CachedUserInfo(null, null, null, null, false);
        }
    }

    /**
     * 处理认证中心用户
     */
    private CachedUserInfo handleAuthCenterUser(Integer authCenterUserId, String username, String email, String jwtUserType) {
        // 1. 先尝试通过用户名查找现有用户
        User existingUser = userService.lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (existingUser != null) {
            log.info("找到现有用户映射 - 本地ID: {}, 用户名: {}", existingUser.getId(), username);
            return new CachedUserInfo(
                existingUser.getId(),
                existingUser.getUsername(),
                existingUser.getEmail(),
                jwtUserType, // 使用JWT中的用户类型
                true
            );
        }

        // 2. 自动注册新用户
        User newUser = autoRegisterUser(authCenterUserId, username, email, jwtUserType);
        if (newUser != null) {
            return new CachedUserInfo(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getEmail(),
                jwtUserType, // 使用JWT中的用户类型
                true
            );
        }

        return new CachedUserInfo(null, null, null, null, false);
    }

    /**
     * 自动注册认证中心用户到本地
     */
    private User autoRegisterUser(Integer authCenterUserId, String username, String email, String userType) {
        log.info("自动注册认证中心用户 - 认证中心ID: {}, 用户名: {}", authCenterUserId, username);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(StringUtils.hasText(email) ? email : username + "@auth-center.local");
        
        // 映射用户类型到数据库
        if ("SUPER_ADMIN".equals(userType)) {
            newUser.setUserType(1); // 管理员
        } else {
            newUser.setUserType(2); // 普通用户
        }

        // 设置默认信息
        newUser.setIntroduction("认证中心用户");
        newUser.setUserStatus(true);

        // 保存用户
        boolean saved = userService.save(newUser);
        if (saved) {
            log.info("用户自动注册成功 - 本地ID: {}, 认证中心ID: {}, 用户名: {}", 
                    newUser.getId(), authCenterUserId, username);
            return newUser;
        } else {
            log.error("用户自动注册失败 - 认证中心ID: {}, 用户名: {}", authCenterUserId, username);
            return null;
        }
    }

    /**
     * 转换用户类型
     */
    private String convertUserType(Integer userType) {
        if (userType == null) {
            return "NORMAL";
        }
        switch (userType) {
            case 0:
            case 1:
                return "ADMIN";
            case 2:
            default:
                return "NORMAL";
        }
    }

    /**
     * 清除用户缓存
     */
    public void clearUserCache(String authSource, Integer authCenterUserId) {
        String cacheKey = USER_CACHE_PREFIX + authSource + ":" + authCenterUserId;
        redisTemplate.delete(cacheKey);
        log.info("已清除用户缓存 - 缓存Key: {}", cacheKey);
    }
}
