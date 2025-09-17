package org.example.amorauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.dto.LoginLogDto;
import org.example.amorauth.entity.LoginLog;
import org.example.amorauth.entity.User;
import org.example.amorauth.mapper.LoginLogMapper;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public void recordLogin(User user, HttpServletRequest request, boolean success, String errorMessage) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(user.getId());
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setIpAddress(getClientIpAddress(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
            loginLog.setLoginType("GOOGLE_OAUTH2");
            loginLog.setSuccess(success);
            loginLog.setErrorMessage(errorMessage);

            loginLogMapper.insertLoginLog(loginLog);

            log.info("Login log recorded for user: {} ({}), Success: {}",
                    user.getEmail(), user.getId(), success);
        } catch (Exception e) {
            log.error("Failed to record login log for user: {}", user.getEmail(), e);
        }
    }

    public void recordLoginAttempt(Long userId, HttpServletRequest request, boolean success, String errorMessage) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setIpAddress(getClientIpAddress(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
            loginLog.setLoginType("GOOGLE_OAUTH2");
            loginLog.setSuccess(success);
            loginLog.setErrorMessage(errorMessage);

            loginLogMapper.insertLoginLog(loginLog);
        } catch (Exception e) {
            log.error("Failed to record login attempt for user ID: {}", userId, e);
        }
    }

    public List<LoginLogDto> getUserLoginHistory(Long userId, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }
        return loginLogMapper.findByUserId(userId, limit);
    }

    public List<LoginLogDto> getRecentLoginLogs(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 20;
        }
        return loginLogMapper.findRecentLogs(limit);
    }

    public long getUserLoginCount(Long userId) {
        return loginLogMapper.countByUserId(userId);
    }

    public LoginLog getLatestLoginByUser(Long userId) {
        return loginLogMapper.findLatestByUserId(userId);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        String xForwardedForCloudflare = request.getHeader("CF-Connecting-IP");
        if (xForwardedForCloudflare != null && !xForwardedForCloudflare.isEmpty()) {
            return xForwardedForCloudflare;
        }

        return request.getRemoteAddr();
    }
}
