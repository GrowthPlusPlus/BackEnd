// 정성우

package me.jsu.springbootdeveloper.token.dto;

import lombok.Getter;
import lombok.Setter;

/*
    엑세스 토큰을 새로 발급하기 위해 리프레시 토큰에 대한 정보만을 가진 클래스이다.
    유저가 주는 리프레시 토큰을 바탕으로 서버가 새로운 엑세스 토큰과 리프레시 토큰을 발급한다.
 */
@Getter
@Setter
public class CreateAccessTokenRequest {
    private String refreshToken;
}
