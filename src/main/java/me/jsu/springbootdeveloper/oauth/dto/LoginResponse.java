// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import me.jsu.springbootdeveloper.user.enumType.UserStatus;

/*
    해당 클래스의 코드는 OAuth 로그인 성공 시 모바일 앱에 반환되는 응답 데이터 구조를 정의한 DTO이다.
    로그인에 성공했음을 알리고 서비스에서 사용할 엑세스 토큰, 리프레시 토큰을 전달하기 위한 응답 객체이다.
    또한 프론트의 화면 분기를 위해 상태도 반환한다.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserStatus userStatus
) {}
