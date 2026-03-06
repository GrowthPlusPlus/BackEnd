// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    사용자가 카카오톡으로부터 code를 받고, 다시 비교 검증하기 위해 우리 서버로 보내는 데이터이다.
 */
@Getter
@NoArgsConstructor
public class KakaoCodeExchangeRequest {
    private String code;
    // [모바일 PKCE 지원] Google과 동일하게 codeVerifier 사용
    private String codeVerifier;

    private String fcmToken;
}
