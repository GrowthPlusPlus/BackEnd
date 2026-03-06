// 정성우

package me.jsu.springbootdeveloper.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
    해당 클래스는 카카오톡 OAuth(PKCE)에 필요한 설정값을 application.yml에서 읽어오기 위한 설정 전용 클래스이다.
 */
@ConfigurationProperties(prefix = "oauth.kakao")
public record KakaoOAuthProperties(
        String clientId,
        String redirectUri
) {}
