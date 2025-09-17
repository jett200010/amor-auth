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

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient() {
        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();

        // 设置自定义的RestTemplate
        RestTemplate restTemplate = oauth2RestTemplate();
        tokenResponseClient.setRestOperations(restTemplate);

        log.info("Custom OAuth2 token response client configured");
        return tokenResponseClient;
    }

    @Bean
    public RestTemplate oauth2RestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // 设置自定义的HTTP请求工厂
        restTemplate.setRequestFactory(clientHttpRequestFactory());

        // 配置消息转换器 - 这是关键修复
        List<HttpMessageConverter<?>> messageConverters = Arrays.asList(
            new FormHttpMessageConverter(),  // 处理表单数据
            new StringHttpMessageConverter(StandardCharsets.UTF_8),  // 处理字符串
            new OAuth2AccessTokenResponseHttpMessageConverter()  // 处理OAuth2响应
        );
        restTemplate.setMessageConverters(messageConverters);

        // 设置OAuth2错误处理器
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

        // 添加请求拦截器来记录详细的HTTP请求信息
        restTemplate.getInterceptors().add((request, body, execution) -> {
            log.info("OAuth2 HTTP Request: {} {}", request.getMethod(), request.getURI());
            log.debug("Request Headers: {}", request.getHeaders());
            if (body != null && body.length > 0) {
                log.debug("Request Body: {}", new String(body, StandardCharsets.UTF_8));
            }

            try {
                var response = execution.execute(request, body);
                log.info("OAuth2 HTTP Response: {} {}", response.getStatusCode(), response.getStatusText());
                log.debug("Response Headers: {}", response.getHeaders());
                return response;
            } catch (Exception e) {
                log.error("OAuth2 HTTP Request failed: {} {}", request.getMethod(), request.getURI(), e);
                throw e;
            }
        });

        log.info("OAuth2 RestTemplate configured with form data support and custom timeouts");
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置更长的超时时间（60秒）
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);

        // 检查系统代理设置
        String httpProxyHost = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxyHost = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");

        // 优先使用HTTPS代理
        if (httpsProxyHost != null && httpsProxyPort != null) {
            try {
                Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(httpsProxyHost, Integer.parseInt(httpsProxyPort)));
                factory.setProxy(proxy);
                log.info("Using HTTPS proxy: {}:{}", httpsProxyHost, httpsProxyPort);
            } catch (NumberFormatException e) {
                log.warn("Invalid HTTPS proxy port: {}", httpsProxyPort);
            }
        }
        // 备用HTTP代理
        else if (httpProxyHost != null && httpProxyPort != null) {
            try {
                Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(httpProxyHost, Integer.parseInt(httpProxyPort)));
                factory.setProxy(proxy);
                log.info("Using HTTP proxy: {}:{}", httpProxyHost, httpProxyPort);
            } catch (NumberFormatException e) {
                log.warn("Invalid HTTP proxy port: {}", httpProxyPort);
            }
        }
        // 尝试常见的VPN代理端口
        else {
            // 常见的本地代理端口
            String[] commonPorts = {"7890", "1080", "8080", "10809"};
            for (String port : commonPorts) {
                if (testProxyConnection("127.0.0.1", port)) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress("127.0.0.1", Integer.parseInt(port)));
                    factory.setProxy(proxy);
                    log.info("Auto-detected and using local proxy: 127.0.0.1:{}", port);
                    break;
                }
            }
        }

        log.info("HTTP Client factory configured - Connect timeout: 60s, Read timeout: 60s");
        return factory;
    }

    private boolean testProxyConnection(String host, String portString) {
        try {
            int port = Integer.parseInt(portString);
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new InetSocketAddress(host, port), 2000);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
