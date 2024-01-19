package com.ld.poetry.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ld.poetry.entity.User;
import com.ld.poetry.handle.PoetryLoginException;
import com.ld.poetry.handle.PoetryRuntimeException;
import com.ld.poetry.utils.*;


@Aspect
@Component
@Slf4j
public class LoginCheckAspect {

    @Around("@annotation(loginCheck)")
    public Object around(ProceedingJoinPoint joinPoint, LoginCheck loginCheck) throws Throwable {
        String token = PoetryUtil.getToken();
        if (!StringUtils.hasText(token)) {
            throw new PoetryLoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        User user = (User) PoetryCache.get(token);

        if (user == null) {
            throw new PoetryLoginException(CodeMsg.LOGIN_EXPIRED.getMsg());
        }

        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            if (loginCheck.value() == PoetryEnum.USER_TYPE_ADMIN.getCode() || loginCheck.value() == PoetryEnum.USER_TYPE_DEV.getCode()) {
                return PoetryResult.fail("请输入管理员账号！");
            }
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            log.info("请求IP：" + PoetryUtil.getIpAddr(PoetryUtil.getRequest()));
            if (loginCheck.value() == PoetryEnum.USER_TYPE_ADMIN.getCode() && user.getId().intValue() != 1) {
                return PoetryResult.fail("请输入管理员账号！");
            }
        } else {
            throw new PoetryLoginException(CodeMsg.NOT_LOGIN.getMsg());
        }

        if (loginCheck.value() < user.getUserType()) {
            throw new PoetryRuntimeException("权限不足！");
        }

        //重置过期时间
        PoetryCache.put(token, user, CommonConst.TOKEN_EXPIRE);
        if (token.contains(CommonConst.USER_ACCESS_TOKEN)) {
            PoetryCache.put(CommonConst.USER_TOKEN + user.getId(), token, CommonConst.TOKEN_EXPIRE);
        } else if (token.contains(CommonConst.ADMIN_ACCESS_TOKEN)) {
            PoetryCache.put(CommonConst.ADMIN_TOKEN + user.getId(), token, CommonConst.TOKEN_EXPIRE);
        }
        return joinPoint.proceed();
    }
}
