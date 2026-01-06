// 정성우

package me.jsu.springbootdeveloper.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
    해당 클래스는 스프링부트에서 jwt 설정값을 외부 설정 파일(yml)로부터 바인딩하기 위한 구성 클래스이다.
    이 설정을 바탕으로 유저 토큰을 생성한다.
 */
@Setter
@Getter
@Component  // 이 클래스를 빈으로 등록한다. (리턴 값 아님)
@ConfigurationProperties("jwt")  //yml에 jwt 정의 값이 있다면 자동으로 이 클래스에 매핑한다.
public class JwtProperties {
    private String issuer;
    private String secretKey;
}