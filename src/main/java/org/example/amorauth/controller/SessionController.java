package org.example.amorauth.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Session管理控制器
 * 用于处理Session清理和Redis缓存管理
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 清空当前用户Session
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (session != null) {
                String sessionId = session.getId();
                log.info("清理Session: {}", sessionId);
                session.invalidate();
                result.put("success", true);
                result.put("message", "Session已清理");
                result.put("sessionId", sessionId);
            } else {
                result.put("success", true);
                result.put("message", "没有活跃的Session");
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清理Session失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 清理Redis中的所有Session数据
     */
    @PostMapping("/redis/clear/sessions")
    public ResponseEntity<Map<String, Object>> clearRedisSessions() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始清理Redis中的所有Session数据");

            int totalCleared = 0;

            // 清理Spring Session相关的key
            String[] sessionPatterns = {
                "spring:session:sessions:*",
                "spring:session:sessions:expires:*",
                "spring:session:index:*"
            };

            for (String pattern : sessionPatterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    totalCleared += keys.size();
                    log.info("清理Redis key模式 {}: {} 个key", pattern, keys.size());
                }
            }

            result.put("success", true);
            result.put("message", "Redis Session数据清理完成");
            result.put("clearedCount", totalCleared);
            result.put("timestamp", System.currentTimeMillis());

            log.info("Redis Session数据清理完成，共清理 {} 个key", totalCleared);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("清理Redis Session数据失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 清理Redis中的所有OAuth2相关缓存
     */
    @PostMapping("/redis/clear/oauth2")
    public ResponseEntity<Map<String, Object>> clearOAuth2Cache() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("开始清理OAuth2相关缓存");

            int totalCleared = 0;

            // OAuth2相关的缓存模式
            String[] oauth2Patterns = {
                "oauth2:*",
                "auth:*",
                "user:*",
                "google:*",
                "security:*"
            };

            for (String pattern : oauth2Patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    totalCleared += keys.size();
                    log.info("清理OAuth2缓存模式 {}: {} 个key", pattern, keys.size());
                }
            }

            result.put("success", true);
            result.put("message", "OAuth2缓存清理完成");
            result.put("clearedCount", totalCleared);
            result.put("timestamp", System.currentTimeMillis());

            log.info("OAuth2缓存清理完成，共清理 {} 个key", totalCleared);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("清理OAuth2缓存失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 清理Redis中的所有缓存数据（慎用）
     */
    @PostMapping("/redis/clear/all")
    public ResponseEntity<Map<String, Object>> clearAllRedisCache() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.warn("执行全量Redis缓存清理");

            Set<String> allKeys = redisTemplate.keys("*");
            int totalCount = allKeys != null ? allKeys.size() : 0;

            if (allKeys != null && !allKeys.isEmpty()) {
                redisTemplate.delete(allKeys);
                log.warn("已清理所有Redis数据，共 {} 个key", totalCount);
            }

            result.put("success", true);
            result.put("message", "所有Redis缓存已清理");
            result.put("clearedCount", totalCount);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("清理所有Redis缓存失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 获取Redis缓存统计信息
     */
    @GetMapping("/redis/stats")
    public ResponseEntity<Map<String, Object>> getRedisStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 统计不同类型的key数量
            Map<String, Integer> stats = new HashMap<>();

            String[] patterns = {
                "spring:session:*",
                "oauth2:*",
                "auth:*",
                "user:*",
                "google:*"
            };

            int totalKeys = 0;
            for (String pattern : patterns) {
                Set<String> keys = redisTemplate.keys(pattern);
                int count = keys != null ? keys.size() : 0;
                stats.put(pattern, count);
                totalKeys += count;
            }

            // 获取所有key的总数
            Set<String> allKeys = redisTemplate.keys("*");
            int allKeysCount = allKeys != null ? allKeys.size() : 0;

            result.put("success", true);
            result.put("patternStats", stats);
            result.put("totalKeys", allKeysCount);
            result.put("categorizedKeys", totalKeys);
            result.put("otherKeys", allKeysCount - totalKeys);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("获取Redis统计信息失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
