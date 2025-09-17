package org.example.amorauth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.entity.User;
import org.example.amorauth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loginPage() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "请通过Google账户登录");
        response.put("googleLoginUrl", "/oauth2/authorization/google");
        return ResponseEntity.ok(response);
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

    @GetMapping("/network/test")
    public ResponseEntity<Map<String, Object>> testNetworkConnectivity() {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("开始网络连接测试...");

            // 测试DNS解析
            Map<String, Object> dnsTest = testDnsResolution();
            response.put("dns", dnsTest);

            // 测试Google API连接
            Map<String, Object> googleApiTest = testGoogleApiConnectivity();
            response.put("googleApi", googleApiTest);

            // 系统网络信息
            Map<String, Object> systemInfo = getSystemNetworkInfo();
            response.put("system", systemInfo);

            response.put("success", true);
            response.put("timestamp", System.currentTimeMillis());

            log.info("网络连接测试完成");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("网络连接测试失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, Object> testDnsResolution() {
        Map<String, Object> result = new HashMap<>();

        try {
            java.net.InetAddress addr1 = java.net.InetAddress.getByName("oauth2.googleapis.com");
            result.put("oauth2.googleapis.com", addr1.getHostAddress());
            log.info("DNS解析成功: oauth2.googleapis.com -> {}", addr1.getHostAddress());

            java.net.InetAddress addr2 = java.net.InetAddress.getByName("accounts.google.com");
            result.put("accounts.google.com", addr2.getHostAddress());
            log.info("DNS解析成功: accounts.google.com -> {}", addr2.getHostAddress());

            result.put("status", "success");
        } catch (Exception e) {
            log.error("DNS解析失败", e);
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        return result;
    }

    private Map<String, Object> testGoogleApiConnectivity() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 创建带超时的HTTP连接测试
            java.net.URL url = new java.net.URL("https://oauth2.googleapis.com/token");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(10000);    // 10秒读取超时
            connection.setRequestMethod("HEAD");

            long startTime = System.currentTimeMillis();
            int responseCode = connection.getResponseCode();
            long endTime = System.currentTimeMillis();

            result.put("oauth2.googleapis.com", Map.of(
                "responseCode", responseCode,
                "responseTime", (endTime - startTime) + "ms",
                "status", responseCode < 400 ? "success" : "failed"
            ));

            log.info("Google API连接测试: 响应码={}, 响应时间={}ms", responseCode, (endTime - startTime));

        } catch (Exception e) {
            log.error("Google API连接测试失败", e);
            result.put("oauth2.googleapis.com", Map.of(
                "status", "failed",
                "error", e.getMessage(),
                "errorType", e.getClass().getSimpleName()
            ));
        }

        return result;
    }

    private Map<String, Object> getSystemNetworkInfo() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 获取系统代理设置
            result.put("httpProxy", System.getProperty("http.proxyHost", "not set"));
            result.put("httpsProxy", System.getProperty("https.proxyHost", "not set"));
            result.put("noProxy", System.getProperty("http.nonProxyHosts", "not set"));

            // 获取网络接口信息
            java.util.List<String> networkInterfaces = new java.util.ArrayList<>();
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface ni = interfaces.nextElement();
                if (ni.isUp() && !ni.isLoopback()) {
                    networkInterfaces.add(ni.getName() + " (" + ni.getDisplayName() + ")");
                }
            }
            result.put("activeNetworkInterfaces", networkInterfaces);

            // Java网络属性
            result.put("javaNetworkStack", Map.of(
                "preferIPv4Stack", System.getProperty("java.net.preferIPv4Stack", "false"),
                "preferIPv6Addresses", System.getProperty("java.net.preferIPv6Addresses", "false")
            ));

        } catch (Exception e) {
            log.error("获取系统网络信息失败", e);
            result.put("error", e.getMessage());
        }

        return result;
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
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "登出成功");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/debug")
    public ResponseEntity<Map<String, Object>> debugOAuth2Config() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 显示当前的OAuth2配置信息
            Map<String, Object> config = new HashMap<>();
            config.put("clientId", "13566225304-0dol5ppkktl1vpehsenii5cqp2avuktt.apps.googleusercontent.com");
            config.put("redirectUri", "http://localhost:8080/api/auth/google/callback");
            config.put("scope", "profile,email");
            config.put("authorizationUri", "https://accounts.google.com/o/oauth2/auth");
            config.put("tokenUri", "https://oauth2.googleapis.com/token");
            config.put("userInfoUri", "https://www.googleapis.com/oauth2/v2/userinfo");

            response.put("oauth2Config", config);

            // 生成授权URL用于手动测试
            String state = java.util.UUID.randomUUID().toString();
            String authUrl = String.format(
                "https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=%s&scope=%s&state=%s&redirect_uri=%s",
                "13566225304-0dol5ppkktl1vpehsenii5cqp2avuktt.apps.googleusercontent.com",
                java.net.URLEncoder.encode("profile email", "UTF-8"),
                state,
                java.net.URLEncoder.encode("http://localhost:8080/api/auth/google/callback", "UTF-8")
            );

            response.put("manualAuthUrl", authUrl);
            response.put("state", state);

            // 提供手动测试说明
            Map<String, String> instructions = new HashMap<>();
            instructions.put("step1", "访问上面的manualAuthUrl进行授权");
            instructions.put("step2", "授权后会重定向到callback，从URL中获取code参数");
            instructions.put("step3", "使用code调用/api/auth/manual-token-exchange端点来手动交换token");

            response.put("instructions", instructions);
            response.put("success", true);

            log.info("OAuth2调试信息已生成");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("生成OAuth2调试信息失败", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/manual-token-exchange")
    public ResponseEntity<Map<String, Object>> manualTokenExchange(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String state = request.get("state");

        if (code == null || code.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "code参数不能为空"));
        }

        log.info("手动token交换: code={}, state={}", code.substring(0, 10) + "...", state);

        try {
            // 手动构建token请求
            Map<String, Object> tokenRequest = new HashMap<>();
            tokenRequest.put("client_id", "13566225304-0dol5ppkktl1vpehsenii5cqp2avuktt.apps.googleusercontent.com");
            tokenRequest.put("client_secret", "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");
            tokenRequest.put("code", code);
            tokenRequest.put("grant_type", "authorization_code");
            tokenRequest.put("redirect_uri", "http://localhost:8080/api/auth/google/callback");

            log.info("发送token请求参数: {}", tokenRequest);

            // 使用RestTemplate手动调用Google token接口
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            // 设置超时
            org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);
            restTemplate.setRequestFactory(factory);

            // 设置请求头
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            // 构建表单数据
            org.springframework.util.MultiValueMap<String, String> formData =
                new org.springframework.util.LinkedMultiValueMap<>();
            tokenRequest.forEach((key, value) -> formData.add(key, value.toString()));

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> entity =
                new org.springframework.http.HttpEntity<>(formData, headers);

            // 发送请求
            org.springframework.http.ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(
                "https://oauth2.googleapis.com/token",
                entity,
                Map.class
            );

            log.info("Token交换成功: status={}", tokenResponse.getStatusCode());
            log.debug("Token响应: {}", tokenResponse.getBody());

            // 如果成功获取token，继续获取用户信息
            if (tokenResponse.getStatusCode().is2xxSuccessful() && tokenResponse.getBody() != null) {
                String accessToken = (String) tokenResponse.getBody().get("access_token");

                // 获取用户信息
                org.springframework.http.HttpHeaders userInfoHeaders = new org.springframework.http.HttpHeaders();
                userInfoHeaders.setBearerAuth(accessToken);
                org.springframework.http.HttpEntity<?> userInfoEntity = new org.springframework.http.HttpEntity<>(userInfoHeaders);

                org.springframework.http.ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    org.springframework.http.HttpMethod.GET,
                    userInfoEntity,
                    Map.class
                );

                log.info("用户信息获取成功: {}", userInfoResponse.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("tokenResponse", tokenResponse.getBody());
                result.put("userInfo", userInfoResponse.getBody());
                result.put("message", "手动token交换成功");

                return ResponseEntity.ok(result);
            }

            return ResponseEntity.status(500).body(Map.of("error", "Token交换失败", "response", tokenResponse.getBody()));

        } catch (Exception e) {
            log.error("手动token交换失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("errorType", e.getClass().getSimpleName());

            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError =
                    (org.springframework.web.client.HttpClientErrorException) e;
                errorResponse.put("httpStatus", httpError.getStatusCode());
                errorResponse.put("httpBody", httpError.getResponseBodyAsString());
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
