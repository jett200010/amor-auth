package org.example.amorauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class OAuth2HttpClientConfig {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

        // 使用自定义 RestTemplate
        RestTemplate restTemplate = oauth2RestTemplate();
        tokenResponseClient.setRestOperations(restTemplate);

        log.info("Custom OAuth2 token response client configured with proxy 127.0.0.1:10808");
        return tokenResponseClient;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        // 创建带代理的RestTemplate用于获取JWK Set
        RestTemplate restTemplate = oauth2UserInfoRestTemplate();

        // 配置JWT解码器使用代理
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
            .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .restOperations(restTemplate)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();

        log.info("JWT decoder configured with proxy for Google certs endpoint");
        return jwtDecoder;
    }

    @Bean
    public OidcUserService oidcUserService() {
        CustomOidcUserService oidcUserService = new CustomOidcUserService();

        // OIDC用户服务需要设置OAuth2UserService而不是RestTemplate
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService = new DefaultOAuth2UserService();
        RestTemplate restTemplate = oauth2UserInfoRestTemplate();
        ((DefaultOAuth2UserService) oauth2UserService).setRestOperations(restTemplate);

        // 设置代理配置的OAuth2UserService
        oidcUserService.setOauth2UserService(oauth2UserService);

        log.info("Custom OIDC user service configured with proxy 127.0.0.1:10808");
        return oidcUserService;
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService userService = new DefaultOAuth2UserService();

        // 为用户信息服务也配置代理
        RestTemplate restTemplate = oauth2UserInfoRestTemplate();
        userService.setRestOperations(restTemplate);

        // 包装服务以添加自定义处理逻辑
        return request -> {
            log.info("Processing OAuth2 user request for client: {} with proxy",
                    request.getClientRegistration().getClientId());
            log.info("UserInfo URI: {}", request.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());

            try {
                OAuth2User oauth2User = userService.loadUser(request);

                // 详细记录所有用户属性
                log.info("OAuth2User attributes received:");
                oauth2User.getAttributes().forEach((key, value) ->
                    log.info("  {} = {}", key, value));

                // 检查关键属性
                String sub = oauth2User.getAttribute("sub");
                String email = oauth2User.getAttribute("email");
                String name = oauth2User.getAttribute("name");

                if (sub == null || sub.isEmpty()) {
                    log.error("Critical error: 'sub' attribute is null or empty! All attributes: {}",
                        oauth2User.getAttributes());
                    throw new IllegalArgumentException("Google user 'sub' attribute cannot be null");
                }

                if (email == null || email.isEmpty()) {
                    log.warn("Warning: 'email' attribute is null or empty for user with sub: {}", sub);
                }

                log.info("Successfully loaded OAuth2 user through proxy: sub={}, email={}, name={}",
                    sub, email, name);
                return oauth2User;
            } catch (Exception e) {
                log.error("Failed to load OAuth2 user through proxy", e);
                throw e;
            }
        };
    }

    @Bean
    public RestTemplate oauth2RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 设置代理 + 超时
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        // 配置消息转换器
        List<HttpMessageConverter<?>> messageConverters = Arrays.asList(
                new FormHttpMessageConverter(),
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new OAuth2AccessTokenResponseHttpMessageConverter()
        );
        restTemplate.setMessageConverters(messageConverters);

        // 设置OAuth2错误处理器
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        // 添加请求拦截器来记录详细的HTTP请求信息
        restTemplate.getInterceptors().add((request, body, execution) -> {
            log.info("OAuth2 Token HTTP Request: {} {}", request.getMethod(), request.getURI());
            log.debug("Request Headers: {}", request.getHeaders());

            try {
                var response = execution.execute(request, body);
                log.info("OAuth2 Token HTTP Response: {} {}", response.getStatusCode(), response.getStatusText());
                return response;
            } catch (Exception e) {
                log.error("OAuth2 Token HTTP Request failed: {} {}", request.getMethod(), request.getURI(), e);
                throw e;
            }
        });

        log.info("OAuth2 Token RestTemplate configured with proxy and custom timeouts");
        return restTemplate;
    }

    @Bean
    public RestTemplate oauth2UserInfoRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 使用相同的代理配置
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        // 添加请求拦截器来记录用户信息请求
        restTemplate.getInterceptors().add((request, body, execution) -> {
            log.info("OAuth2 UserInfo HTTP Request: {} {}", request.getMethod(), request.getURI());
            log.debug("Request Headers: {}", request.getHeaders());

            try {
                var response = execution.execute(request, body);
                log.info("OAuth2 UserInfo HTTP Response: {} {}", response.getStatusCode(), response.getStatusText());
                return response;
            } catch (Exception e) {
                log.error("OAuth2 UserInfo HTTP Request failed: {} {}", request.getMethod(), request.getURI(), e);
                throw e;
            }
        });

        log.info("OAuth2 UserInfo RestTemplate configured with proxy");
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置更长的超时时间（60秒）
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);

        // 固定代理配置
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10808));
        factory.setProxy(proxy);

        log.info("Using fixed HTTP proxy: 127.0.0.1:10808");

        return factory;
    }
}
