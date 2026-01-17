package com.ld.poetry.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP客户端工具类 - 统一管理外部API调用
 * 替代原生HttpURLConnection，提供更好的性能和错误处理
 */
@Component
@Slf4j
public class HttpClientUtil {
    
    private final RestTemplate restTemplate;
    
    public HttpClientUtil() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * POST请求（表单数据）
     */
    public String postForm(String url, Map<String, String> params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            StringBuilder body = new StringBuilder();
            params.forEach((key, value) -> {
                if (body.length() > 0) {
                    body.append("&");
                }
                body.append(key).append("=").append(value);
            });
            
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("POST请求失败: {}", url, e);
            throw new RuntimeException("HTTP请求失败", e);
        }
    }
    
    /**
     * GET请求
     */
    public String get(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("GET请求失败: {}", url, e);
            throw new RuntimeException("HTTP请求失败", e);
        }
    }
    
    /**
     * POST请求（JSON）
     */
    public String postJson(String url, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            return response.getBody();
        } catch (Exception e) {
            log.error("POST JSON请求失败: {}", url, e);
            throw new RuntimeException("HTTP请求失败", e);
        }
    }
}
