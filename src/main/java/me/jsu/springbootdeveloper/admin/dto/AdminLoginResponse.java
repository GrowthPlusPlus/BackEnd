// 정성우

package me.jsu.springbootdeveloper.admin.dto;

import lombok.Builder;
import lombok.Getter;

/*
    관리자 로그인에 성공한 경우, 엑세스/리프레시 토큰을 반환해준다.
 */
@Getter
@Builder
public class AdminLoginResponse {
    public String accessToken;
    public String refreshToken;
    public boolean fcmTokenRegistered;
}
