// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
    생성된 리프레시 토큰, 엑세스 토큰을 유저에게 보내기 위해 정보를 정제한 클래스이다.
 */
@AllArgsConstructor
@Getter
public class TokenResponse {  
    private String accessToken;
    private String refreshToken;
}
