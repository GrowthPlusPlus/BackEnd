// 정성우

package me.jsu.springbootdeveloper.dto.oauth;

public record GoogleCodeExchangeRequest(
        String code,
        String codeVerifier
) {}
