// 정성우

package me.jsu.springbootdeveloper.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/*
    해당 클래스는 Google OAuth(PKCE)에 필요한 설정값을 application.yml에서 읽어오기 위한 설정 전용 클래스이다.
 */
@ConfigurationProperties(prefix = "spring.oauth.google")  // oauth.google로 시작하는 설정들을 이 클래스의 필드에 자동으로 바인딩 한다.
public record GoogleOAuthProperties(  // record는 getter와 생성자를 자동으로 생성하고 불변하는 데이터 객체를 만든다.
        // Android + iOS 허용 목록으로 공개되어도 되는 값이다. id token의 검증에 사용한다.
        String androidClientId,
        String androidRedirectUri,
        String iosClientId,
        String iosRedirectUri
) {}