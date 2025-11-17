package com.ld.poetry.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.ld.poetry.annotation.RequirePermission;
import com.ld.poetry.auth.AuthContext;
import com.ld.poetry.auth.SimpleAuthHelper;
import com.ld.poetry.entity.User;
import com.ld.poetry.enums.PermissionCode;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.service.UserService;
import com.ld.poetry.utils.CommonConst;
import com.ld.poetry.utils.PoetryCache;
import com.ld.poetry.utils.PoetryUtil;
import com.ld.poetry.vo.UserVO;

import java.util.List;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private SimpleAuthHelper simpleAuthHelper;


    /**
     * 用户名/密码注册
     */
    @PostMapping("/regist")
    public PoetryResult<UserVO> regist(@Validated @RequestBody UserVO user) {
        return userService.regist(user);
    }


    /**
     * 用户名、邮箱、手机号/密码登录
     */
    @PostMapping("/login")
    public PoetryResult<UserVO> login(@RequestParam("account") String account,
                                      @RequestParam("password") String password,
                                      @RequestParam(value = "isAdmin", defaultValue = "false") Boolean isAdmin) {
        return userService.login(account, password, isAdmin);
    }


    /**
     * Token登录
     */
    @PostMapping("/token")
    public PoetryResult<UserVO> login(@RequestParam("userToken") String userToken) {
        return userService.token(userToken);
    }

    /**
     * 根据Token获取用户信息 - 用户Profile自查
     * 自动解析token获得用户ID映射，获取本业务真实ID的用户信息
     */
    @GetMapping("/info")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult<UserVO> getUserInfo() {
        try {
            // 从当前认证上下文获取用户信息
            AuthContext.UserInfo currentUser = AuthContext.getCurrentUser();
            if (currentUser == null) {
                return PoetryResult.fail("用户未登录");
            }

            // 获取本地用户ID（已经通过token映射得到）
            Integer localUserId = currentUser.getUserId();
            
            // 通过本地用户ID获取完整的用户信息
            User user = userService.getById(localUserId);
            if (user == null) {
                return PoetryResult.fail("用户信息不存在");
            }

            // 转换为UserVO并补充认证相关信息
            UserVO userInfo = new UserVO();
            userInfo.setId(user.getId());
            userInfo.setUsername(user.getUsername());
            userInfo.setEmail(user.getEmail());
            userInfo.setPhoneNumber(user.getPhoneNumber());
            userInfo.setIntroduction(user.getIntroduction());
            userInfo.setAvatar(user.getAvatar());
            userInfo.setGender(user.getGender());
            userInfo.setCreateTime(user.getCreateTime());
            userInfo.setUpdateTime(user.getUpdateTime());
            userInfo.setUpdateBy(user.getUpdateBy());
            
            // 设置访问令牌（如果需要的话）
            userInfo.setAccessToken(null); // 不返回敏感信息
            
            return PoetryResult.success(userInfo);
            
        } catch (Exception e) {
            return PoetryResult.fail("获取用户信息失败: " + e.getMessage());
        }
    }


    /**
     * 退出
     */
    @GetMapping("/logout")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult exit() {
        return userService.exit();
    }


    /**
     * 更新用户信息
     */
    @PostMapping("/updateUserInfo")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult<UserVO> updateUserInfo(@RequestBody UserVO user) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateUserInfo(user);
    }

    /**
     * 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCode")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult getCode(@RequestParam("flag") Integer flag) {
        return userService.getCode(flag);
    }

    /**
     * 绑定手机号或者邮箱
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForBind")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult getCodeForBind(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForBind(place, flag);
    }

    /**
     * 更新邮箱、手机号、密码
     * <p>
     * 1 手机号
     * 2 邮箱
     * 3 密码：place=老密码&password=新密码
     */
    @PostMapping("/updateSecretInfo")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult<UserVO> updateSecretInfo(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam(value = "code", required = false) String code, @RequestParam("password") String password) {
        PoetryCache.remove(CommonConst.USER_CACHE + PoetryUtil.getUserId().toString());
        return userService.updateSecretInfo(place, flag, code, password);
    }

    /**
     * 忘记密码 获取验证码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @GetMapping("/getCodeForForgetPassword")
    public PoetryResult getCodeForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag) {
        return userService.getCodeForForgetPassword(place, flag);
    }

    /**
     * 忘记密码 更新密码
     * <p>
     * 1 手机号
     * 2 邮箱
     */
    @PostMapping("/updateForForgetPassword")
    public PoetryResult updateForForgetPassword(@RequestParam("place") String place, @RequestParam("flag") Integer flag, @RequestParam("code") String code, @RequestParam("password") String password) {
        return userService.updateForForgetPassword(place, flag, code, password);
    }

    /**
     * 根据用户名查找用户信息
     */
    @GetMapping("/getUserByUsername")
    @RequirePermission(PermissionCode.LOGIN_REQUIRED)
    public PoetryResult<List<UserVO>> getUserByUsername(@RequestParam("username") String username) {
        return userService.getUserByUsername(username);
    }
}

