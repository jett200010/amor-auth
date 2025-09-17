package org.example.amorauth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public Map<String, Object> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "欢迎使用 Amor Auth Google OAuth2 认证服务");
        response.put("googleLoginUrl", "/oauth2/authorization/google");
        response.put("endpoints", Map.of(
            "login", "/oauth2/authorization/google",
            "userInfo", "/api/auth/user",
            "logout", "/logout"
        ));
        return response;
    }
}
