package com.ld.poetry.service.impl;

import cn.hutool.crypto.SecureUtil;

import com.baomidou.mybatisplus.core.toolkit.AES;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.UserMapper;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.entity.WeiYan;
import com.ld.poetry.handle.PoetryRuntimeException;
import com.ld.poetry.im.http.dao.ImChatGroupUserMapper;
import com.ld.poetry.im.http.dao.ImChatUserFriendMapper;
import com.ld.poetry.im.http.entity.ImChatGroupUser;
import com.ld.poetry.im.http.entity.ImChatUserFriend;
import com.ld.poetry.im.websocket.ImConfigConst;
import com.ld.poetry.im.websocket.TioWebsocketStarter;
import com.ld.poetry.service.UserService;
import com.ld.poetry.service.WeiYanService;
import com.ld.poetry.utils.*;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.vo.UserVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.tio.core.Tio;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 用户信息表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-12
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private WeiYanService weiYanService;

    @Autowired
    private ImChatGroupUserMapper imChatGroupUserMapper;

    @Autowired
    private ImChatUserFriendMapper imChatUserFriendMapper;

    @Autowired
    private TioWebsocketStarter tioWebsocketStarter;

    @Autowired
    private MailUtil mailUtil;

    @Value("${user.code.format}")
    private String codeFormat;

    @Override
    public PoetryResult<UserVO> login(String account, String password, Boolean isAdmin) {
        password = new String(AES.encrypt(password, CommonConst.CRYPOTJS_KEY));

        User one = lambdaQuery().and(wrapper -> wrapper
                .eq(User::getUsername, account)
                .or()
                .eq(User::getEmail, account)
                .or()
                .eq(User::getPhoneNumber, account))
                .eq(User::getPassword, DigestUtils.md5DigestAsHex(password.getBytes()))
                .one();

        if (one == null) {
            return PoetryResult.fail("账号/密码错误，请重新输入！");
        }

        if (!one.getUserStatus()) {
            return PoetryResult.fail("账号被冻结！");
        }

        if (isAdmin) {
            if (one.getUserType() != PoetryEnum.USER_TYPE_ADMIN.getCode() && one.getUserType() != PoetryEnum.USER_TYPE_DEV.getCode()) {
                return PoetryResult.fail("请输入管理员账号！");
            }
            // JWT是无状态的，不需要清除旧token
        }

        // 生成JWT token - 根据用户类型确定权限
        String userType;
        if (one.getId().equals(CommonConst.ADMIN_ID) || one.getId().equals(1)) {
            // 超级管理员 (支持ID为1和983341575的用户)
            userType = CommonConst.USER_TYPE_SUPER_ADMIN;
        } else if (isAdmin && (one.getUserType() == PoetryEnum.USER_TYPE_ADMIN.getCode() || one.getUserType() == PoetryEnum.USER_TYPE_DEV.getCode())) {
            // 管理员
            userType = CommonConst.USER_TYPE_ADMIN;
        } else {
            // 普通用户
            userType = CommonConst.USER_TYPE_NORMAL;
        }
        
        JwtUtil jwtUtil = SpringContextUtil.getBean(JwtUtil.class);
        String jwtToken = jwtUtil.generateToken(one.getId(), one.getUsername(), one.getEmail(), userType);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        if (isAdmin && one.getUserType() == PoetryEnum.USER_TYPE_ADMIN.getCode()) {
            userVO.setIsBoss(true);
        }

        userVO.setAccessToken(jwtToken);
        return PoetryResult.success(userVO);
    }

    @Override
    public PoetryResult exit() {
        Integer userId = PoetryUtil.getUserId();
        if (userId != null) {
            // 对于JWT，我们不需要从缓存中删除token，因为token是无状态的
            // 只需要移除WebSocket连接
            Tio.removeUser(tioWebsocketStarter.getServerTioConfig(), String.valueOf(userId), "remove user");
        }
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<UserVO> regist(UserVO user) {
        String regex = "\\d{11}";
        if (user.getUsername().matches(regex)) {
            return PoetryResult.fail("用户名不能为11位数字！");
        }

        if (user.getUsername().contains("@")) {
            return PoetryResult.fail("用户名不能包含@！");
        }

        user.setPassword(new String(AES.encrypt(user.getPassword(), CommonConst.CRYPOTJS_KEY)));

        Integer count = lambdaQuery().eq(User::getUsername, user.getUsername()).count();
        if (count != 0) {
            return PoetryResult.fail("用户名重复！");
        }
        User u = new User();
        BeanUtils.copyProperties(user, u);
        u.setEmail(null);
        u.setPhoneNumber(null);
        u.setPassword(DigestUtils.md5DigestAsHex(u.getPassword().getBytes()));
        if (!StringUtils.hasText(u.getAvatar())) {
            u.setAvatar(PoetryUtil.getRandomAvatar(null));
        }
        save(u);
        User one = lambdaQuery().eq(User::getId, u.getId()).one();

        // 生成JWT token
        JwtUtil jwtUtil = SpringContextUtil.getBean(JwtUtil.class);
        String jwtToken = jwtUtil.generateToken(one.getId(), one.getUsername(), one.getEmail(), CommonConst.USER_TYPE_NORMAL);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        userVO.setAccessToken(jwtToken);

        // WeiYan weiYan = new WeiYan();
        // weiYan.setUserId(one.getId());
        // weiYan.setContent("到此一游");
        // weiYan.setType(CommonConst.WEIYAN_TYPE_FRIEND);
        // weiYan.setIsPublic(Boolean.TRUE);
        // weiYanService.save(weiYan);

        // ImChatGroupUser imChatGroupUser = new ImChatGroupUser();
        // imChatGroupUser.setGroupId(ImConfigConst.DEFAULT_GROUP_ID);
        // imChatGroupUser.setUserId(one.getId());
        // imChatGroupUser.setUserStatus(ImConfigConst.GROUP_USER_STATUS_PASS);
        // imChatGroupUserMapper.insert(imChatGroupUser);

        // ImChatUserFriend imChatUser = new ImChatUserFriend();
        // imChatUser.setUserId(one.getId());
        // imChatUser.setFriendId(PoetryUtil.getAdminUser().getId());
        // imChatUser.setRemark("站长");
        // imChatUser.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        // imChatUserFriendMapper.insert(imChatUser);
        
        // ImChatUserFriend imChatFriend = new ImChatUserFriend();
        // imChatFriend.setUserId(PoetryUtil.getAdminUser().getId());
        // imChatFriend.setFriendId(one.getId());
        // imChatFriend.setFriendStatus(ImConfigConst.FRIEND_STATUS_PASS);
        // imChatUserFriendMapper.insert(imChatFriend);

        return PoetryResult.success(userVO);
    }

    @Override
    public PoetryResult<UserVO> updateUserInfo(UserVO user) {
        if (StringUtils.hasText(user.getUsername())) {
            String regex = "\\d{11}";
            if (user.getUsername().matches(regex)) {
                return PoetryResult.fail("用户名不能为11位数字！");
            }

            if (user.getUsername().contains("@")) {
                return PoetryResult.fail("用户名不能包含@！");
            }

            Integer count = lambdaQuery().eq(User::getUsername, user.getUsername()).ne(User::getId, PoetryUtil.getUserId()).count();
            if (count != 0) {
                return PoetryResult.fail("用户名重复！");
            }
        }
        User u = new User();
        BeanUtils.copyProperties(user, u);
        u.setId(PoetryUtil.getUserId());
        u.setPassword(null);
        u.setEmail(null);
        u.setPhoneNumber(null);
        updateById(u);
        User one = lambdaQuery().eq(User::getId, u.getId()).one();
        // JWT是无状态的，不需要更新缓存

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        return PoetryResult.success(userVO);
    }

    @Override
    public PoetryResult getCode(Integer flag) {
        // 由于JWT是无状态的，我们需要从数据库获取用户信息
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            return PoetryResult.fail("用户未登录");
        }
        User user = lambdaQuery().eq(User::getId, userId).one();
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            if (!StringUtils.hasText(user.getPhoneNumber())) {
                return PoetryResult.fail("请先绑定手机号！");
            }

            log.info(user.getId() + "---" + user.getUsername() + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            if (!StringUtils.hasText(user.getEmail())) {
                return PoetryResult.fail("请先绑定邮箱！");
            }

            log.info(user.getId() + "---" + user.getUsername() + "---" + "邮箱验证码---" + i);

            List<String> mail = new ArrayList<>();
            mail.add(user.getEmail());
            String text = getCodeMail(i);
            WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
            mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? "寻国记" : webInfo.getWebName()) + "的回执！", text);
        }
        PoetryCache.put(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + flag, Integer.valueOf(i), 300);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult getCodeForBind(String place, Integer flag) {
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            log.info(place + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            log.info(place + "---" + "邮箱验证码---" + i);
            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(i);
            WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
            mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? "寻国记" : webInfo.getWebName()) + "的回执！", text);
        }
        PoetryCache.put(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag, Integer.valueOf(i), 300);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<UserVO> updateSecretInfo(String place, Integer flag, String code, String password) {
        password = new String(AES.encrypt(password, CommonConst.CRYPOTJS_KEY));

        User user = PoetryUtil.getCurrentUser();
        if ((flag == 1 || flag == 2) && !DigestUtils.md5DigestAsHex(password.getBytes()).equals(user.getPassword())) {
            return PoetryResult.fail("密码错误！");
        }
        if ((flag == 1 || flag == 2) && !StringUtils.hasText(code)) {
            return PoetryResult.fail("请输入验证码！");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        if (flag == 1) {
            Integer count = lambdaQuery().eq(User::getPhoneNumber, place).count();
            if (count != 0) {
                return PoetryResult.fail("手机号重复！");
            }
            Integer codeCache = (Integer) PoetryCache.get(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                PoetryCache.remove(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);

                updateUser.setPhoneNumber(place);
            } else {
                return PoetryResult.fail("验证码错误！");
            }

        } else if (flag == 2) {
            Integer count = lambdaQuery().eq(User::getEmail, place).count();
            if (count != 0) {
                return PoetryResult.fail("邮箱重复！");
            }
            Integer codeCache = (Integer) PoetryCache.get(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);
            if (codeCache != null && codeCache.intValue() == Integer.parseInt(code)) {

                PoetryCache.remove(CommonConst.USER_CODE + PoetryUtil.getUserId() + "_" + place + "_" + flag);

                updateUser.setEmail(place);
            } else {
                return PoetryResult.fail("验证码错误！");
            }
        } else if (flag == 3) {
            if (DigestUtils.md5DigestAsHex(place.getBytes()).equals(user.getPassword())) {
                updateUser.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
            } else {
                return PoetryResult.fail("密码错误！");
            }
        }
        updateById(updateUser);

        User one = lambdaQuery().eq(User::getId, user.getId()).one();
        // JWT是无状态的，不需要更新缓存

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(one, userVO);
        userVO.setPassword(null);
        return PoetryResult.success(userVO);
    }

    @Override
    public PoetryResult getCodeForForgetPassword(String place, Integer flag) {
        int i = new Random().nextInt(900000) + 100000;
        if (flag == 1) {
            log.info(place + "---" + "手机验证码---" + i);
        } else if (flag == 2) {
            log.info(place + "---" + "邮箱验证码---" + i);

            List<String> mail = new ArrayList<>();
            mail.add(place);
            String text = getCodeMail(i);
            WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
            mailUtil.sendMailMessage(mail, "您有一封来自" + (webInfo == null ? "寻国记" : webInfo.getWebName()) + "的回执！", text);
        }
        PoetryCache.put(CommonConst.FORGET_PASSWORD + place + "_" + flag, Integer.valueOf(i), 300);
        return PoetryResult.success();
    }

    @Override
    public PoetryResult updateForForgetPassword(String place, Integer flag, String code, String password) {
        password = new String(AES.encrypt(password, CommonConst.CRYPOTJS_KEY));

        Integer codeCache = (Integer) PoetryCache.get(CommonConst.FORGET_PASSWORD + place + "_" + flag);
        if (codeCache == null || codeCache != Integer.parseInt(code)) {
            return PoetryResult.fail("验证码错误！");
        }

        PoetryCache.remove(CommonConst.FORGET_PASSWORD + place + "_" + flag);

        if (flag == 1) {
            User user = lambdaQuery().eq(User::getPhoneNumber, place).one();
            if (user == null) {
                return PoetryResult.fail("该手机号未绑定账号！");
            }

            if (!user.getUserStatus()) {
                return PoetryResult.fail("账号被冻结！");
            }

            lambdaUpdate().eq(User::getPhoneNumber, place).set(User::getPassword, DigestUtils.md5DigestAsHex(password.getBytes())).update();
            PoetryCache.remove(CommonConst.USER_CACHE + user.getId().toString());
        } else if (flag == 2) {
            User user = lambdaQuery().eq(User::getEmail, place).one();
            if (user == null) {
                return PoetryResult.fail("该邮箱未绑定账号！");
            }

            if (!user.getUserStatus()) {
                return PoetryResult.fail("账号被冻结！");
            }

            lambdaUpdate().eq(User::getEmail, place).set(User::getPassword, DigestUtils.md5DigestAsHex(password.getBytes())).update();
            PoetryCache.remove(CommonConst.USER_CACHE + user.getId().toString());
        }

        return PoetryResult.success();
    }

    @Override
    public PoetryResult<Page> listUser(BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<User> lambdaQuery = lambdaQuery();

        if (baseRequestVO.getUserStatus() != null) {
            lambdaQuery.eq(User::getUserStatus, baseRequestVO.getUserStatus());
        }

        if (baseRequestVO.getUserType() != null) {
            lambdaQuery.eq(User::getUserType, baseRequestVO.getUserType());
        }

        if (StringUtils.hasText(baseRequestVO.getKeywords())) {
            lambdaQuery.and(lq -> lq.eq(User::getUsername, baseRequestVO.getKeywords())
                    .or()
                    .eq(User::getPhoneNumber, baseRequestVO.getKeywords()));
        }

        lambdaQuery.orderByDesc(User::getCreateTime).page((Page)baseRequestVO);

        List<User> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            records.forEach(u -> {
                u.setPassword(null);
                u.setOpenId(null);
            });
        }
        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<List<UserVO>> getUserByUsername(String username) {
        List<User> users = lambdaQuery().select(User::getId, User::getUsername, User::getAvatar, User::getGender, User::getIntroduction).like(User::getUsername, username).last("limit 5").list();
        List<UserVO> userVOS = users.stream().map(u -> {
            UserVO userVO = new UserVO();
            userVO.setId(u.getId());
            userVO.setUsername(u.getUsername());
            userVO.setAvatar(u.getAvatar());
            userVO.setIntroduction(u.getIntroduction());
            userVO.setGender(u.getGender());
            return userVO;
        }).collect(Collectors.toList());
        return PoetryResult.success(userVOS);
    }

    @Override
    public PoetryResult<UserVO> token(String userToken) {
        userToken = new String(AES.encrypt(userToken, CommonConst.CRYPOTJS_KEY));

        if (!StringUtils.hasText(userToken)) {
            throw new PoetryRuntimeException("未登陆，请登陆后再进行操作！");
        }

        User user = (User) PoetryCache.get(userToken);

        if (user == null) {
            throw new PoetryRuntimeException("登录已过期，请重新登陆！");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setPassword(null);

        userVO.setAccessToken(userToken);

        return PoetryResult.success(userVO);
    }

    private String getCodeMail(int i) {
        WebInfo webInfo = (WebInfo) PoetryCache.get(CommonConst.WEB_INFO);
        String webName = (webInfo == null ? "JANXLAND" : webInfo.getWebName());
        return String.format(MailUtil.mailText,
                webName,
                String.format(MailUtil.imMail, PoetryUtil.getAdminUser().getUsername()),
                PoetryUtil.getAdminUser().getUsername(),
                String.format(codeFormat, i),
                "",
                webName);
    }
}
