// 정성우

package me.jsu.springbootdeveloper.dto.oauth;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {}
