// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
    해당 클래스는 토큰 로테이션에서 사용된다.
 */
@AllArgsConstructor
@Getter
public class TokenPair {
    private String accessToken;
    private String refreshToken;
}
