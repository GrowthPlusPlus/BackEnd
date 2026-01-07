// 정성우

package me.jsu.springbootdeveloper.dto.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
    이 클래스는 내 서버가 Google Token Endpoint에 요청을 보낸 뒤,
    구글 서버로부터 받은 토큰 응답(JSON)을 자바 객체로 매핑하기 위한 DTO이다.

    즉, 구글 서버 → 내 서버 통신 결과를 담는 응답 DTO로, 구글 서버에서의 OAuth인증이 완료됐음을 의미한다.
    이 dto값을 기반으로 내 서버에 접속할 토큰(JWT)을 만든다.

    구글 토큰 엔드포인트의 응답 json에 매핑하기 위해 @JsonProperty를 사용한다.
 */
public record GoogleTokenResponse(
        // 구글 엑세스 토큰으로, 구글 리소스에 접근할 수 있지만 우리 서버의 인증에는 사용하지 않는다. 따라서 앱에 전달하지 않는다.
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,  // 구글 리프레시 토큰이다.
        @JsonProperty("id_token") String idToken,  // 구글 사용자의 이메일, 이름, id를 포함한 값이다.
        @JsonProperty("expires_in") Integer expiresIn,  // 구글 엑세스 토큰의 만료 시간이다.
        @JsonProperty("token_type") String tokenType  // 토큰의 타입으로 일반적으로 "Bearer"이다.
) {}
