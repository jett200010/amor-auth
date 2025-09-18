package org.example.amorauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.entity.User;
import org.example.amorauth.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 允许跨域访问
public class GoogleLoginController {

    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    /**
     * 获取Google登录URL - 前端调用此接口获取登录链接
     */
    @GetMapping("/login-url")
    public ResponseEntity<Map<String, Object>> getGoogleLoginUrl() {
        Map<String, Object> response = new HashMap<>();

        // Google OAuth2授权URL
        String googleLoginUrl = "/oauth2/authorization/google";

        response.put("success", true);
        response.put("loginUrl", googleLoginUrl);
        response.put("message", "请使用此URL进行Google登录");
        response.put("clientId", googleClientId);

        log.info("Frontend requested Google login URL");
        return ResponseEntity.ok(response);
    }

    /**
     * 直接跳转到Google登录页面
     */
    @GetMapping("/login")
    public void redirectToGoogleLogin(HttpServletResponse response) throws IOException {
        log.info("Redirecting to Google OAuth2 authorization");
        response.sendRedirect("/oauth2/authorization/google");
    }

    /**
     * Google登录回调处理 - 这是Google OAuth2的回调地址
     */
    @GetMapping("/callback")
    public ResponseEntity<Map<String, Object>> handleGoogleCallback(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        log.info("Google login callback received");

        if (oauth2User == null) {
            log.warn("OAuth2User is null in callback");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Google登录失败，未获取到用户信息"
            ));
        }

        try {
            // 获取Google用户信息
            String googleId = oauth2User.getAttribute("sub");
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String picture = oauth2User.getAttribute("picture");
            String locale = oauth2User.getAttribute("locale");

            log.info("Processing Google login for user: {}", email);

            // 创建或更新用户
            User user = userService.createOrUpdateUser(googleId, email, name, picture, locale);

            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Google登录成功");
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "picture", user.getPicture() != null ? user.getPicture() : "",
                "googleId", user.getGoogleId()
            ));
            response.put("token", generateJwtToken(user)); // 如果你有JWT实现的话

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing Google login callback", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "处理Google登录时发生错误: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        if (oauth2User == null) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "用户未登录"
            ));
        }

        try {
            String googleId = oauth2User.getAttribute("sub");
            User user = userService.findByGoogleId(googleId);

            if (user == null) {
                return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "用户不存在"
                ));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "picture", user.getPicture() != null ? user.getPicture() : "",
                "googleId", user.getGoogleId(),
                "createdAt", user.getCreatedAt()
//                "lastLoginAt", user.getLastLoginAt()
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting current user info", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "获取用户信息失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 登出接口
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        // 这里可以清除session或token
        log.info("User logout requested");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登出成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 检查登录状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> checkLoginStatus(
            @AuthenticationPrincipal OAuth2User oauth2User) {

        Map<String, Object> response = new HashMap<>();

        if (oauth2User != null) {
            response.put("isLoggedIn", true);
            response.put("email", oauth2User.getAttribute("email"));
            response.put("name", oauth2User.getAttribute("name"));
        } else {
            response.put("isLoggedIn", false);
        }

        return ResponseEntity.ok(response);
    }

    // 简单的JWT生成方法（如果需要的话）
    private String generateJwtToken(User user) {
        // 这里你可以集成JWT库来生成token
        // 暂时返回一个简单的标识
        return "jwt_token_for_user_" + user.getId();
    }
}
