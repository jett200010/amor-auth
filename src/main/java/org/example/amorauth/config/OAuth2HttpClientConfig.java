package org.example.amorauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class OAuth2HttpClientConfig {

    @Value("${oauth2.proxy.enabled:false}")
    private boolean proxyEnabled;

    @Value("${oauth2.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${oauth2.proxy.port:10808}")
    private int proxyPort;

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

        // 使用自定义 RestTemplate
        RestTemplate restTemplate = oauth2RestTemplate();
        tokenResponseClient.setRestOperations(restTemplate);

        if (proxyEnabled) {
            log.info("Custom OAuth2 token response client configured with proxy {}:{}", proxyHost, proxyPort);
        } else {
            log.info("Custom OAuth2 token response client configured without proxy (production mode)");
        }
        return tokenResponseClient;
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

                return oauth2User;
            } catch (Exception e) {
                log.error("Failed to load OAuth2 user with proxy: {}", e.getMessage());
                log.error("Error details:", e);
                throw e;
            }
        };
    }

    @Bean
    public RestTemplate oauth2RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 配置代理
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 根据环境配置代理
        if (proxyEnabled) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
            log.info("OAuth2 RestTemplate configured with proxy {}:{}", proxyHost, proxyPort);
        } else {
            log.info("OAuth2 RestTemplate configured without proxy (production mode)");
        }

        // 设置超时
        factory.setConnectTimeout(60000); // 60秒
        factory.setReadTimeout(60000);    // 60秒

        restTemplate.setRequestFactory(factory);

        // 配置消息转换器
        List<HttpMessageConverter<?>> messageConverters = Arrays.asList(
            new OAuth2AccessTokenResponseHttpMessageConverter(),
            new FormHttpMessageConverter(),
            new StringHttpMessageConverter(StandardCharsets.UTF_8)
        );
        restTemplate.setMessageConverters(messageConverters);

        // 设置错误处理器
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        return restTemplate;
    }

    @Bean
    public RestTemplate oauth2UserInfoRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 配置代理
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 根据环境配置代理 - 生产环境(AWS)不使用代理
        if (proxyEnabled) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            factory.setProxy(proxy);
            log.info("OAuth2 UserInfo RestTemplate configured with proxy {}:{}", proxyHost, proxyPort);
        } else {
            log.info("OAuth2 UserInfo RestTemplate configured without proxy (production mode)");
        }

        // 设置超时
        factory.setConnectTimeout(60000); // 60秒
        factory.setReadTimeout(60000);    // 60秒

        restTemplate.setRequestFactory(factory);

        return restTemplate;
    }
}
