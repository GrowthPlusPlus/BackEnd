// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
    네이버 서버로부터 OAuth 인증이 완료되면, 인증된 유저에 관한 정보를 네이버 서버로부터 받기 위한 dto이다.
 */
public record NaverTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") String expiresIn
) {}
