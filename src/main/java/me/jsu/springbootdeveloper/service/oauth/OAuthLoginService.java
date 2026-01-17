// 정성우

package me.jsu.springbootdeveloper.service.oauth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.config.oauth.GoogleIdTokenVerifyService;
import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthTokenService;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.oauth.GoogleTokenResponse;
import me.jsu.springbootdeveloper.dto.oauth.LoginResponse;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.token.RefreshTokenService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    해당 클래스는 Google OAuth와 PKCE 로그인 유스케이스 전체를 담당하는 Service 계층 클래스이다.
    로그인 유스케이스를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final GoogleOAuthTokenService tokenService;
    private final GoogleIdTokenVerifyService idTokenVerifyService;  // 구글 id 토큰 검증을 위한 라이브러리
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    /*
        Google OAuth (Authorization Code + PKCE) 로그인의 유스케이스를 한 번에 수행하는 함수이다.
     */
    public LoginResponse loginWithGoogle(String code, String codeVerifier) {
        /*
            Google Token(Authorization Code + PKCE)을 교환하여 구글이 PKCE(codeVerifier)를 검증한다.
            검증 성공 시, 구글이 access_token, id_token를 반환한다.
         */
        GoogleTokenResponse googleToken = tokenService.exchangeCode(code, codeVerifier);
        // 구글에서 온 토큰 응답이 유효하지 않으면 OAuth 로그인을 실패로 하고 해당 오류 처리를 위한 OAuthLoginException를 생성한다.
        if (googleToken == null || googleToken.idToken() == null) {
            throw new OAuthLoginException("INVALID_AUTH_CODE");  // OAuth 로그인 실패라는 예외를 발생시킨다. 이유는 INVALID_AUTH_CODE 이다.
        }

        /*
            공개 키를 기반으로 jwt 서명 (iss, aud, exp)을 검증한다. ID Token 검증 과정이다. (OIDC)
            구글에서 제공하는 기능으로 checked exception로 설계되어 반드시 에러 처리를 해야 한다.

            GoogleIdToken.Payload는 Google ID Token(JWT)의 내용물(Claims)을 담는 객체로 검증이 끝난 사용자 정보가 들어있다.
         */
        GoogleIdToken.Payload payload;
        try {
            payload = idTokenVerifyService.verify(googleToken.idToken());  // 정상적인 토큰인 경우 payload로 추출한다.
        } catch (Exception e) {
            throw new OAuthLoginException("INVALID_ID_TOKEN");  // 문제가 생긴 경우, 구체적인 실패 원인 코드를 담아 예외처리 한다.
        }

        // 검증된 사용자의 구글 이메일, 이름 정보를 추출한다.
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        /*
            사용자 조회 또는 생성하는 단계로 우리 서비스에 최초 로그인 시 신규 생성, 기존 사용자면 조회한다.
            최초 닉네임은 구글에서 받은 닉네임이 들어가지만 추후에 닉네임을 수정해도 토큰 발급에는 아무런 문제가 없다.
         */
        User user = userService.findOrCreate(email, name);

        // 우리 서비스의 엑세스 토큰을 발급한다.
        String accessToken = tokenProvider.generateToken(user, Duration.ofHours(1));
        // 우리 서비스의 리프레시 토큰을 발급한다. 30일마다 oauth 로그인으로 새로 발급해야 한다.
        String refreshToken = tokenProvider.generateToken(user, Duration.ofDays(30));

        // Refresh Token을 우리 서비스의 서버에 저장한다.
        refreshTokenService.saveForOAuth(user, refreshToken);

        // 생성한 리프레시 토큰과 엑세스 토큰을 LoginResponse 클래스 형태로 가공하여 컨트롤러에 반환한다.
        return new LoginResponse(accessToken, refreshToken);
    }
}