// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.oauth.dto.KakaoTokenResponse;
import me.jsu.springbootdeveloper.oauth.dto.KakaoUserResponse;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/*
    카카오톡으로 로그인하는 경우 사용되는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class OAuthKakaoService {
    private final OAuthLoginService oauthLoginService;
    private final KakaoOAuthTokenService tokenService;
    private final WebClient webClient = WebClient.builder().build();

    /*
        구글과 동일한 작업을 카카오로 수행하는 함수이다.
     */
    public LoginResponse loginWithKakao(String code, String codeVerifier, String fcmToken) {
        // 유저 정보에 접근할 수 있는 카카오 서버의 토큰을 얻는다.
        KakaoTokenResponse kakaoToken = tokenService.exchangeCode(code, codeVerifier);

        // 카카오에서 온 토큰 응답이 유효하지 않은 경우이다.
        if (kakaoToken == null || kakaoToken.accessToken() == null) {
            throw new OAuthLoginException("INVALID_KAKAO_AUTH_CODE");
        }

        // Google과 다른 부분으로, 구글은 id_token 방식으로 정보를 가져왔지만 카카오톡은 user/me API를 호출한다.
        KakaoUserResponse userInfo = getUserInfo(kakaoToken.accessToken());
        String email = String.valueOf(userInfo.getId());  // 카카오톡 고유 id를 email로 취급하여 String으로 변환한다.

        return oauthLoginService.loginWithOAuth(email, fcmToken);
    }

    /*
        카카오에서 받은 토큰을 바탕으로 유저의 정보를 검색하는 함수이다.
     */
    public KakaoUserResponse getUserInfo(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserResponse.class)
                    .block();
        }
        catch (WebClientResponseException e) {
            throw new OAuthLoginException("KAKAO_USER_INFO_FAILED");
        }
    }
}
