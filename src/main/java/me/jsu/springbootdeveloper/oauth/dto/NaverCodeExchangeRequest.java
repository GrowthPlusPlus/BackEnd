// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    사용자가 네이버로부터 code를 받고, 다시 비교 검증하기 위해 우리 서버로 보내는 데이터이다.
 */
@Getter
@NoArgsConstructor
public class NaverCodeExchangeRequest {
    private String code;
    private String state;
    private String fcmToken;
}
