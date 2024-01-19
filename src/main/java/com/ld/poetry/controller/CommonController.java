package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.TreeHoleMapper;import com.ld.poetry.dao.WebInfoMapper;
import com.ld.poetry.im.websocket.TioWebsocketStarter;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.service.CommentService;
import com.ld.poetry.service.UserService;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common")
public class CommonController {

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private WebInfoMapper webInfoMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TreeHoleMapper treeHoleMapper;

    @Autowired
    private TioWebsocketStarter tioWebsocketStarter;

    @GetMapping("/gethtml")
    public Object listBossTreeHole(@RequestParam("url") String url) {
        try {
            return Jsoup.connect(url).ignoreContentType(true).
                    header("Content-Type", "application/json;charset=UTF-8").get().text();
        }catch(Exception e) {
            return PoetryResult.fail("发生了一些错误！");
        }
    }
}
