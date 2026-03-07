// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.oauth.dto.NaverTokenResponse;
import me.jsu.springbootdeveloper.oauth.dto.NaverUserResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/*
    네이버로 로그인하는 경우 사용되는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class OAuthNaverService {
    private final OAuthLoginService oauthLoginService;
    private final NaverOAuthTokenService tokenService;
    private final WebClient webClient = WebClient.builder().build();

    public LoginResponse loginWithNaver(String code, String state, String fcmToken) {
        if (code == null || code.isBlank()) {
            throw new OAuthLoginException("NAVER_CODE_MISSING");
        }

        // 네이버에 code 검증을 요청한다.
        NaverTokenResponse naverToken = tokenService.exchangeCode(code, state);

        if (naverToken == null || naverToken.accessToken() == null) {
            throw new OAuthLoginException("INVALID_NAVER_AUTH_CODE");
        }

        // Google과 다른 부분으로, 구글은 id_token 방식으로 정보를 가져왔지만 네이버는 user/me API를 호출한다.
        NaverUserResponse userInfo = getUserInfo(naverToken.accessToken());

        if (!"00".equals(userInfo.getResultCode())) {  // 유저 정보를 성공적으로 가져왔는지 검사한다.
            throw new OAuthLoginException("NAVER_USER_INFO_FAILED");
        }

        String email = userInfo.getEmailSafe();
        if (email == null || email.isBlank()) {  // 이메일 제공 동의를 하지 않아 값이 없는 경우 예외처리 한다.
            throw new OAuthLoginException("NAVER_EMAIL_NOT_PROVIDED");
        }

        return oauthLoginService.loginWithOAuth(email, fcmToken);
    }

    public NaverUserResponse getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://openapi.naver.com/v1/nid/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(NaverUserResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new OAuthLoginException("NAVER_USER_INFO_FAILED");
        }
    }
}
