// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
    카카오톡 서버로부터 OAuth 인증이 완료되면, 인증된 유저에 관한 정보를 카카오톡 서버로부터 받기 위한 dto이다.
 */
public record KakaoTokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,  // 카카오톡 리프레시 토큰이다.
        @JsonProperty("expires_in")
        Integer expiresIn,
        @JsonProperty("token_type")
        String tokenType  // 일반적으로 Bearer 형식이다.
) {}
