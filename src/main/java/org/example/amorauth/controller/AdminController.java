package org.example.amorauth.controller;

import lombok.RequiredArgsConstructor;
import org.example.amorauth.dto.LoginLogDto;
import org.example.amorauth.service.LoginLogService;
import org.example.amorauth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LoginLogService loginLogService;
    private final UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats(@AuthenticationPrincipal OAuth2User oauth2User) {
        String googleId = oauth2User.getAttribute("sub");
        var user = userService.findByGoogleId(googleId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogins", loginLogService.getUserLoginCount(user.getId()));
        stats.put("userInfo", user);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/login-history")
    public ResponseEntity<List<LoginLogDto>> getLoginHistory(
            @AuthenticationPrincipal OAuth2User oauth2User,
            @RequestParam(defaultValue = "10") Integer limit) {

        String googleId = oauth2User.getAttribute("sub");
        var user = userService.findByGoogleId(googleId);

        List<LoginLogDto> loginHistory = loginLogService.getUserLoginHistory(user.getId(), limit);
        return ResponseEntity.ok(loginHistory);
    }

    @GetMapping("/recent-logins")
    public ResponseEntity<List<LoginLogDto>> getRecentLogins(
            @RequestParam(defaultValue = "20") Integer limit) {

        List<LoginLogDto> recentLogins = loginLogService.getRecentLoginLogs(limit);
        return ResponseEntity.ok(recentLogins);
    }
}
