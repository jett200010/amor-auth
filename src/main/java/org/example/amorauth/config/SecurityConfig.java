package org.example.amorauth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.service.LoginLogService;
import org.example.amorauth.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserService userService;
    private final LoginLogService loginLogService;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/api/auth/login", "/oauth2/**", "/login/oauth2/**",
                    "/api/auth/google/callback", "/error", "/api/auth/network/test",
                    "/api/google/**","/session/*").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/api/auth/google/callback")
                )
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(authorizationCodeTokenResponseClient)
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService)
                )
                .successHandler((request, response, authentication) -> {
                    log.info("OAuth2 login success for user: {}", authentication.getName());

                    // OAuth2登录成功后的处理
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    var user = userService.processOAuth2User(oauth2User);

                    // 记录成功登录日志
                    loginLogService.recordLogin(user, request, true, null);

                    // 返回JSON响应而不是重定向，避免循环
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(200);
                    response.getWriter().write("{\"success\":true,\"message\":\"登录成功\",\"redirectUrl\":\"/dashboard\"}");
                })
                .failureHandler((request, response, exception) -> {
                    log.error("OAuth2 login failed", exception);

                    // OAuth2登录失败后的处理
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.setStatus(401);
                    response.getWriter().write("{\"success\":false,\"message\":\"登录失败\",\"error\":\"" + exception.getMessage() + "\"}");
                })
            )
            .csrf(csrf -> csrf.disable())
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"success\":true,\"message\":\"登出成功\"}");
                })
                .permitAll()
            );

        return http.build();
    }
}
