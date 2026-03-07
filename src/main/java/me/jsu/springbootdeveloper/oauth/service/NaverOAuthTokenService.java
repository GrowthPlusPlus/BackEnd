// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.oauth.NaverOAuthProperties;
import me.jsu.springbootdeveloper.oauth.dto.NaverTokenResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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
                    .bodyValue(
                            "grant_type=authorization_code" +
                                    "&client_id=" + props.clientId() +
                                    "&client_secret=" + props.clientSecret() +
                                    "&code=" + code +
                                    "&state=" + state +
                                    "&redirect_uri=" + props.redirectUri()
                    )
                    .retrieve()
                    .bodyToMono(NaverTokenResponse.class)
                    .block();
        }
        catch (WebClientResponseException e) {
            throw new OAuthLoginException("NAVER_TOKEN_EXCHANGE_FAILED");
        }
    }
}
