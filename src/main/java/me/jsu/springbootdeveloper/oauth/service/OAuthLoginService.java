// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    해당 클래스는 OAuth와 PKCE 로그인 유스케이스 전체를 담당하는 Service 계층 클래스이다.
    로그인 유스케이스를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class OAuthLoginService {
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final FcmService fcmService;  // FCM 토큰 등록을 위한 서비스

    /*
        구글, 카카오톡 등 플랫폼 상관 없이 공통적으로 거치는 함수이다.
     */
    public LoginResponse loginWithOAuth(String email, String fcmToken) {
        /*
            사용자 조회 또는 생성하는 단계로 우리 서비스에 최초 로그인 시 신규 생성, 기존 사용자면 조회한다.
            최초 닉네임은 랜덤으로 생성된 닉네임이 들어간다.
         */
        User user = userService.findOrCreate(email);

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