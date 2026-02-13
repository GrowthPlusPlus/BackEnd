// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.oauth.dto.GoogleTokenResponse;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
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
    private final GoogleOAuthProperties googleOAuthProperties;
    private final FcmService fcmService;  // FCM 토큰 등록을 위한 서비스

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
        }
        else if (clientId.equals("433865217738-gu6vtmh4d7dpephj0mb152vm4s3vcap9.apps.googleusercontent.com")) {
            redirectUri = "com.googleusercontent.apps.433865217738-gu6vtmh4d7dpephj0mb152vm4s3vcap9:/oauth2redirect";
        }
        else {
            throw new OAuthLoginException("INVALID_CLIENT_ID");
        }

        /*
            Google Token(Authorization Code + PKCE)을 교환하여 구글이 PKCE(codeVerifier)를 검증한다.
            검증 성공 시, 구글이 access_token, id_token를 반환한다.
         */
        GoogleTokenResponse googleToken = tokenService.exchangeCode(code, codeVerifier, clientId, redirectUri);
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

        /*
            FCM 토큰이 제공된 경우, 로그인 성공 시 자동으로 등록한다.
            FCM 토큰은 앱에서 Firebase SDK를 통해 생성되며, 로그인 요청과 함께 전달될 수 있다.
            프론트엔드 개발자 편의를 위해 로그인 시 FCM 토큰을 함께 보낼 수 있도록 옵셔널로 지원한다.
         */
        boolean fcmTokenRegistered = false;
        if (fcmToken != null && !fcmToken.isBlank()) {
            try {
                fcmService.registerOrUpdateToken(user.getEmail(), fcmToken);
                fcmTokenRegistered = true;
            } catch (Exception e) {
                // FCM 토큰 등록 실패해도 로그인은 성공으로 처리한다.
                // FCM 토큰은 나중에 별도 API로 등록할 수 있으므로 로그인 실패로 처리하지 않는다.
                // 로그만 남기고 계속 진행한다.
            }
        } else {
            // FCM 토큰이 로그인 요청에 포함되지 않은 경우, 기존에 등록된 토큰이 있는지 확인한다.
            fcmTokenRegistered = (user.getFcmToken() != null && !user.getFcmToken().isBlank());
        }

        /*
            생성한 리프레시 토큰과 엑세스 토큰을 LoginResponse 클래스 형태로 가공하여 컨트롤러에 반환한다.
            또한 처음 우리 서비스에 로그인한 사람에게 초기 설정을 할 수 있도록 상태 값도 반환한다.
            fcmTokenRegistered 플래그를 포함하여 프론트엔드가 FCM 토큰 등록이 필요한지 알 수 있도록 한다.
         */
        return new LoginResponse(accessToken, refreshToken, user.getUserStatus(), fcmTokenRegistered);
    }
}