package com.ld.poetry.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * JSON工具类 - 替代fastjson，使用Jackson
 */
@Slf4j
public class JsonUtil {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * JSON字符串转List
     */
    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, 
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            log.error("JSON转List失败: {}", json, e);
            return null;
        }
    }
    
    /**
     * JSON字符串转对象（支持泛型）
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", json, e);
            return null;
        }
    }
}
