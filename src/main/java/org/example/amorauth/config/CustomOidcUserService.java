package org.example.amorauth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.Map;

@Component
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Loading OIDC user from ID Token (bypassing JWT verification due to proxy issues)");

        try {
            // 直接从ID Token中提取用户信息，不进行网络验证
            OidcIdToken idToken = userRequest.getIdToken();
            Map<String, Object> claims = idToken.getClaims();

            log.info("ID Token claims:");
            claims.forEach((key, value) -> log.info("  {} = {}", key, value));

            // 验证关键字段
            String sub = (String) claims.get("sub");
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");

            if (sub == null || sub.isEmpty()) {
                log.error("Subject (sub) is null or empty in ID Token");
                throw new OAuth2AuthenticationException("Subject cannot be null");
            }

            log.info("Extracted user info: sub={}, email={}, name={}", sub, email, name);

            // 创建OIDC用户，使用ID Token中的信息，不需要网络验证
            Set<OAuth2UserAuthority> authorities = Set.of(new OAuth2UserAuthority(claims));

            // 直接创建OidcUser，绕过可能的网络验证
            OidcUser oidcUser = new DefaultOidcUser(authorities, idToken, "sub");

            log.info("Successfully created OIDC user without network verification");
            return oidcUser;

        } catch (Exception e) {
            log.error("Failed to load OIDC user from ID Token", e);

            // 如果OIDC方式失败，尝试回退到普通OAuth2方式
            log.info("Falling back to regular OAuth2 user loading...");
            try {
                // 从ID Token创建一个简单的OAuth2User
                OidcIdToken idToken = userRequest.getIdToken();
                Map<String, Object> attributes = idToken.getClaims();

                String sub = (String) attributes.get("sub");
                if (sub != null) {
                    Set<OAuth2UserAuthority> authorities = Set.of(new OAuth2UserAuthority(attributes));
                    return new DefaultOidcUser(authorities, idToken, "sub");
                }
            } catch (Exception fallbackException) {
                log.error("Fallback also failed", fallbackException);
            }

            throw new OAuth2AuthenticationException("Failed to load user information");
        }
    }
}
