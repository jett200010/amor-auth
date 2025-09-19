package org.example.amorauth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.entity.User;
import org.example.amorauth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public void redirectToGoogleLogin(HttpServletResponse response) throws IOException {
        log.info("Redirecting to Google OAuth2 authorization");
        response.sendRedirect("/oauth2/authorization/google");
    }

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> loginSuccess(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.badRequest().build();
        }

        String googleId = oauth2User.getAttribute("sub");
        User user = userService.findByGoogleId(googleId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "登录成功");
        response.put("user", user);

        log.info("User logged in successfully: {}", user.getEmail());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, String>> loginError() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "登录失败");
        response.put("error", "认证过程中发生错误");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Map<String, Object>> googleCallback(@AuthenticationPrincipal OAuth2User oauth2User) {
        log.info("Google callback endpoint accessed, OAuth2User present: {}", oauth2User != null);

        if (oauth2User == null) {
            log.warn("OAuth2User is null in callback");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "OAuth2认证失败");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // 获取Google用户信息
            String googleId = oauth2User.getAttribute("sub");
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String picture = oauth2User.getAttribute("picture");
            String locale = oauth2User.getAttribute("locale");

            log.info("Google OAuth2 callback received for user: {}, googleId: {}", email, googleId);

            // 创建或更新用户
            User user = userService.createOrUpdateUser(googleId, email, name, picture, locale);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", user);
            response.put("redirectUrl", "/dashboard");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing Google callback", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "处理登录回调时发生错误: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User == null) {
            return ResponseEntity.status(401).build();
        }

        String googleId = oauth2User.getAttribute("sub");
        User user = userService.findByGoogleId(googleId);

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("authenticated", true);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            @AuthenticationPrincipal OAuth2User oauth2User) {

        log.info("开始执行登出操作");

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取当前用户信息
            String userInfo = "未知用户";
            if (oauth2User != null) {
                userInfo = oauth2User.getAttribute("email");
                log.info("用户 {} 正在登出", userInfo);
            }

            // 清理Spring Security上下文
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
                logoutHandler.logout(request, response, auth);
                log.info("Spring Security上下文已清理");
            }

            // 清理HTTP Session
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.info("正在清理Session: {}", session.getId());
                session.invalidate();
                log.info("HTTP Session已清理");
            }

            // 设置响应头，确保客户端清理缓存
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            result.put("success", true);
            result.put("message", "登出成功");
            result.put("user", userInfo);
            result.put("timestamp", System.currentTimeMillis());

            log.info("用户 {} 登出成功", userInfo);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("登出过程中发生错误", e);
            result.put("success", false);
            result.put("message", "登出过程中发生错误");
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
