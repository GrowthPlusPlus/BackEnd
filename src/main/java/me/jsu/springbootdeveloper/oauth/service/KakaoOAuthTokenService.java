// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.oauth.KakaoOAuthProperties;
import me.jsu.springbootdeveloper.oauth.dto.KakaoTokenResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
    유저에게 받은 code를 카카오톡 서버에 보내서 유저를 검증하는 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class KakaoOAuthTokenService {
    private final WebClient webClient = WebClient.builder().build();
    private final KakaoOAuthProperties props;

    public KakaoTokenResponse exchangeCode(String code, String codeVerifier) {
        try {
            return webClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                            BodyInserters.fromFormData("grant_type", "authorization_code")
                                    .with("client_id", props.clientId())
                                    .with("client_secret", props.clientSecret())
                                    .with("redirect_uri", props.redirectUri())
                                    .with("code", code)
                                    // [PKCE] Google과 동일하게 code_verifier 전달
                                    .with("code_verifier", codeVerifier)
                    )
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();
        }
        catch (WebClientResponseException e) {
            // 토큰 교환 실패 사유(invalid_client / invalid_grant 등)를 카카오 응답 바디에서 확인하기 위함이다.
            System.out.println("Kakao token endpoint error status=" + e.getStatusCode());
            System.out.println("Kakao token endpoint error body=" + e.getResponseBodyAsString());

            throw new OAuthLoginException("KAKAO_TOKEN_EXCHANGE_FAILED");
        }
    }
}
