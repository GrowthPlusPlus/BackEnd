// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import me.jsu.springbootdeveloper.oauth.dto.GoogleTokenResponse;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import org.springframework.stereotype.Service;

/*
    구글로 로그인하는 경우 사용되는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class OAuthGoogleService {
    private final OAuthLoginService oauthLoginService;
    private final GoogleOAuthTokenService tokenService;
    private final GoogleIdTokenVerifyService idTokenVerifyService;  // 구글 id 토큰 검증을 위한 라이브러리
    private final GoogleOAuthProperties props;

    /*
        Google OAuth (Authorization Code + PKCE) 로그인의 유스케이스를 한 번에 수행하는 함수이다.
        로그인 시 FCM 토큰이 함께 전달되면 자동으로 등록한다.

        @param code: 구글 Authorization Server가 발급한 authorization code
        @param codeVerifier: PKCE에서 사용하는 code_verifier
        @param clientId: OAuth 클라이언트 ID
        @param fcmToken: Firebase Cloud Messaging 토큰 (옵셔널). 제공되면 로그인 성공 시 자동으로 등록된다.
     */
    public LoginResponse loginWithGoogle(String code, String codeVerifier, String clientId, String fcmToken) {
        if (clientId == null) {  // client ID가 null이면 예외처리한다.
            throw new OAuthLoginException("CLIENT_ID_MISSING");
        }

        // clientId 검증과 함께 redirectUri 주소를 정의한다.
        String redirectUri;
        if (clientId.equals("433865217738-m3uqqdv9lumpf1ne8e3bkpsbtsa6919i.apps.googleusercontent.com")) {
            redirectUri = "com.googleusercontent.apps.433865217738-m3uqqdv9lumpf1ne8e3bkpsbtsa6919i:/oauth2redirect";
        } else if (clientId.equals("433865217738-gu6vtmh4d7dpephj0mb152vm4s3vcap9.apps.googleusercontent.com")) {
            redirectUri = "com.googleusercontent.apps.433865217738-gu6vtmh4d7dpephj0mb152vm4s3vcap9:/oauth2redirect";
        } else {
            throw new OAuthLoginException("INVALID_CLIENT_ID");
        }

//        if (clientId.equals(props.androidClientId())) {
//            redirectUri = props.androidRedirectUri();
//        }
//        else if (clientId.equals(props.iosClientId())) {
//            redirectUri = props.iosRedirectUri();
//        }
//        else {
//            throw new OAuthLoginException("INVALID_CLIENT_ID");
//        }

        /*
            Google Token(Authorization Code + PKCE)을 교환하여 구글이 PKCE(codeVerifier)를 검증한다.
            검증 성공 시, 구글이 access_token, id_token를 반환한다.
         */
        GoogleTokenResponse googleToken = tokenService.exchangeCode(code, codeVerifier, clientId, redirectUri);
        // 구글에서 온 토큰 응답이 유효하지 않으면 OAuth 로그인을 실패로 하고 해당 오류 처리를 위한 OAuthLoginException를 생성한다.
        if (googleToken == null || googleToken.idToken() == null) {
            throw new OAuthLoginException("INVALID_GOOGLE_AUTH_CODE");  // OAuth 로그인 실패라는 예외를 발생시킨다. 이유는 INVALID_AUTH_CODE 이다.
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

        // 검증된 사용자의 구글 이메일과 이름을 추출하여 공통 로그인 서비스에 넘겨준다.
        return oauthLoginService.loginWithOAuth(payload.getEmail(), fcmToken);
    }
}
