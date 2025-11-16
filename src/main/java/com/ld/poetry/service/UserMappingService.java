package com.ld.poetry.service;

import com.ld.poetry.entity.User;
import com.ld.poetry.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 扁平化用户映射服务
 * 负责认证中心用户与本地用户的映射和自动注册
 */
@Slf4j
@Service
public class UserMappingService {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 根据JWT token获取或创建本地用户
     * @param token JWT token
     * @return 本地用户信息
     */
    public User getOrCreateLocalUser(String token) {
        try {
            // 解析token信息
            String authSource = jwtUtil.getAuthSourceFromToken(token);
            Integer authCenterUserId = jwtUtil.getAuthCenterUserIdFromToken(token);
            String username = jwtUtil.getUsernameFromToken(token);
            String email = jwtUtil.getEmailFromToken(token);
            String userType = jwtUtil.getUserTypeFromToken(token);

            log.info("处理用户映射 - 认证来源: {}, 认证中心ID: {}, 用户名: {}", 
                    authSource, authCenterUserId, username);

            // 如果是本项目token，直接获取用户
            if (!"auth-center".equals(authSource)) {
                Integer localUserId = jwtUtil.getUserIdFromToken(token);
                return userService.getById(localUserId);
            }

            // 认证中心用户处理
            return handleAuthCenterUser(authCenterUserId, username, email, userType);

        } catch (Exception e) {
            log.error("用户映射失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 处理认证中心用户
     */
    private User handleAuthCenterUser(Integer authCenterUserId, String username, String email, String userType) {
        // 1. 先尝试通过用户名查找现有用户
        User existingUser = userService.lambdaQuery()
                .eq(User::getUsername, username)
                .one();

        if (existingUser != null) {
            log.info("找到现有用户映射 - 本地ID: {}, 用户名: {}", existingUser.getId(), username);
            return existingUser;
        }

        // 2. 自动注册新用户
        return autoRegisterUser(authCenterUserId, username, email, userType);
    }

    /**
     * 自动注册认证中心用户到本地
     */
    private User autoRegisterUser(Integer authCenterUserId, String username, String email, String userType) {
        log.info("自动注册认证中心用户 - 认证中心ID: {}, 用户名: {}", authCenterUserId, username);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(StringUtils.hasText(email) ? email : username + "@auth-center.local");
        
        // 映射用户类型
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
     * 获取本地用户ID
     * @param token JWT token
     * @return 本地用户ID
     */
    public Integer getLocalUserId(String token) {
        User localUser = getOrCreateLocalUser(token);
        return localUser != null ? localUser.getId() : null;
    }
}