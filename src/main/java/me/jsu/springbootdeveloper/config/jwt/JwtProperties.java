package me.jsu.springbootdeveloper.config.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component  // 이 클래스를 빈으로 등록함 (리턴 값 아님)
@ConfigurationProperties("jwt")  //yml에 jwt 정의 값이 있다면 자동으로 이 클래스에 매핑함
public class JwtProperties {
    private String issuers;
    private String secretKey;
}

// 이 설정을 바탕으로 유저 토큰들을 생성함