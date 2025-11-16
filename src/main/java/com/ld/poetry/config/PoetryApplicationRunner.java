package com.ld.poetry.config;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.ld.poetry.dao.WebInfoMapper;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.WebInfo;
import com.ld.poetry.im.websocket.TioWebsocketStarter;
import com.ld.poetry.service.UserService;
import com.ld.poetry.utils.CommonConst;
import com.ld.poetry.utils.CommonQuery;
import com.ld.poetry.utils.PoetryCache;
import com.ld.poetry.utils.PoetryEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class PoetryApplicationRunner implements ApplicationRunner {

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private UserService userService;

    @Autowired
    private TioWebsocketStarter tioWebsocketStarter;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 异步初始化缓存，不阻塞启动
        new Thread(() -> {
            try {
                // 初始化网站信息缓存
                LambdaQueryChainWrapper<WebInfo> wrapper = new LambdaQueryChainWrapper<>(webInfoMapper);
                List<WebInfo> list = wrapper.list();
                if (!CollectionUtils.isEmpty(list)) {
                    PoetryCache.put(CommonConst.WEB_INFO, list.get(0));
                }

                // 初始化分类信息缓存
                List<Sort> sortInfo = commonQuery.getSortInfo();
                if (!CollectionUtils.isEmpty(sortInfo)) {
                    PoetryCache.put(CommonConst.SORT_INFO, sortInfo);
                }

                // 初始化管理员信息缓存
                User admin = userService.lambdaQuery().eq(User::getUserType, PoetryEnum.USER_TYPE_ADMIN.getCode()).one();
                PoetryCache.put(CommonConst.ADMIN, admin);
                
                System.out.println("缓存初始化完成");
            } catch (Exception e) {
                System.err.println("缓存初始化失败: " + e.getMessage());
            }
        }).start();

        // WebSocket启动也异步化
        new Thread(() -> {
            try {
                tioWebsocketStarter.start();
                System.out.println("WebSocket启动完成");
            } catch (Exception e) {
                System.err.println("WebSocket启动失败: " + e.getMessage());
            }
        }).start();
        
        System.out.println("应用启动完成，后台初始化中...");
    }
}
