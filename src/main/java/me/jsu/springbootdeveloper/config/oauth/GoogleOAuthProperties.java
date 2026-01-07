// 정성우

package me.jsu.springbootdeveloper.config.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/*
    해당 클래스는 Google OAuth(PKCE)에 필요한 설정값을 application.yml에서 읽어오기 위한 설정 전용 클래스이다.
 */
@ConfigurationProperties(prefix = "oauth.google")  // oauth.google로 시작하는 설정들을 이 클래스의 필드에 자동으로 바인딩 한다.
public record GoogleOAuthProperties(  // record는 getter와 생성자를 자동으로 생성하고 불변하는 데이터 객체를 만든다.
        String clientId,  // 공개되어도 되는 값으로 id token의 검증에 사용한다.
        String clientSecret,  // 클라이언트에 절대 노출되면 안되는 값이다.
        String redirectUri  // 인증이 끝난 뒤 내 앱으로 돌아올 주소이다.
) {}