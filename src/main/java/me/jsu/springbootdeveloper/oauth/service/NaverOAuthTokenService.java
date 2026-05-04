// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.oauth.NaverOAuthProperties;
import me.jsu.springbootdeveloper.oauth.dto.NaverTokenResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/*
    유저에게 받은 code를 네이버 서버에 보내서 유저를 검증하는 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class NaverOAuthTokenService {
    private final NaverOAuthProperties props;
    private final WebClient webClient = WebClient.builder().build();

    public NaverTokenResponse exchangeCode(String code, String state) {
        try {
            return webClient.post()
                    .uri("https://nid.naver.com/oauth2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(
                            BodyInserters.fromFormData("grant_type", "authorization_code")
                                    .with("client_id", props.clientId())
                                    .with("client_secret", props.clientSecret())
                                    .with("redirect_uri", props.redirectUri())
                                    .with("code", code)
                                    .with("state", state)
                    )
                    .retrieve()
                    .bodyToMono(NaverTokenResponse.class)
                    .block();
        }
        catch (WebClientResponseException e) {
            // 토큰 교환 실패 사유(invalid_client / invalid_grant 등)를 네이버 응답 바디에서 확인하기 위함이다.
            System.out.println("Naver token endpoint error status=" + e.getStatusCode());
            System.out.println("Naver token endpoint error body=" + e.getResponseBodyAsString());

            throw new OAuthLoginException("NAVER_TOKEN_EXCHANGE_FAILED");
        }
    }
}
