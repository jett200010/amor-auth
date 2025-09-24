package org.example.amorauth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.amorauth.common.constant.HttpStatus;
import org.example.amorauth.common.domain.R;
import org.example.amorauth.entity.User;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {


    private final UserService userService;
    private final LoginLogService loginLogService;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // 开放登录与回调等
                        .requestMatchers("/", "/api/auth/login", "/oauth2/**", "/login/oauth2/**",
                                "/api/auth/google/callback", "/error").permitAll()
                        // 管理接口强制认证
                        .requestMatchers("/api/admin/**").authenticated()
                        // 其他接口按需放开或限制
                        .anyRequest().permitAll()
                )
                // API 统一返回 JSON，不做重定向
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(apiAuthenticationEntryPoint(),
                                new AntPathRequestMatcher("/api/**"))
                        .accessDeniedHandler(apiAccessDeniedHandler())
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
                    User user = userService.processOAuth2User(oauth2User);

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

    private AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            var body = R.fail(HttpStatus.UNAUTHORIZED, "未授权访问");
            objectMapper.writeValue(response.getWriter(), body);
        };
    }

    private AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            var body = R.fail(HttpStatus.FORBIDDEN, "无权限访问");
            objectMapper.writeValue(response.getWriter(), body);
        };
    }
}
