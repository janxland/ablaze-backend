package com.ld.poetry.controller;

import com.ld.poetry.config.PoetryResult;
import org.jsoup.Jsoup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.net.URL;

@RestController
@Tag(name = "Common", description = "通用接口")
@RequestMapping("/common")
public class CommonController {

    /**
     * 抓取公开网页文本
     * <p>
     * SSRF 防护：仅允许 http/https，且解析后的目标 IP 不得为
     * 回环/私有/链路本地/保留网段（防止打 127.0.0.1、内网 Redis/MySQL、云元数据）。
     */
    @Operation(summary = "抓取公开网页文本(仅公网 http/https)")
    @GetMapping("/gethtml")
    public Object listBossTreeHole(@RequestParam("url") String url) {
        try {
            URL u = new URL(url);
            String protocol = u.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return PoetryResult.fail("仅支持 http/https");
            }
            for (InetAddress addr : InetAddress.getAllByName(u.getHost())) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress()
                        || addr.isLinkLocalAddress() || addr.isAnyLocalAddress()
                        || addr.isMulticastAddress()) {
                    return PoetryResult.fail("不允许访问内网地址");
                }
            }
            return Jsoup.connect(url).ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .timeout(8000)
                    .get().text();
        } catch (Exception e) {
            return PoetryResult.fail("发生了一些错误！");
        }
    }
}
