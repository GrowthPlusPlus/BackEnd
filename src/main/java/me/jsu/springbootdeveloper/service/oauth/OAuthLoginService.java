// 정성우

package me.jsu.springbootdeveloper.service.oauth;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.config.oauth.GoogleIdTokenVerifyService;
import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthTokenService;
import me.jsu.springbootdeveloper.domain.RefreshToken;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.oauth.GoogleTokenResponse;
import me.jsu.springbootdeveloper.dto.oauth.LoginResponse;
import me.jsu.springbootdeveloper.repository.RefreshTokenRepository;
import me.jsu.springbootdeveloper.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {

    private final GoogleOAuthTokenService tokenService;
    private final GoogleIdTokenVerifyService idTokenVerifyService;
    private final UserService userService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Google OAuth (Authorization Code + PKCE) 로그인 유스케이스
     */
    public LoginResponse loginWithGoogle(
            String code,
            String codeVerifier
    ) throws Exception {

        // 1️⃣ Authorization Code + PKCE → Google Token
        GoogleTokenResponse googleToken =
                tokenService.exchangeCode(code, codeVerifier);

        if (googleToken == null || googleToken.idToken() == null) {
            throw new OAuthLoginException("INVALID_AUTH_CODE");
        }

        // 2️⃣ ID Token 검증 (OIDC)
        var payload =
                idTokenVerifyService.verify(googleToken.idToken());

        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // 3️⃣ 사용자 조회 또는 생성
        User user = userService.findOrCreate(email, name);

        // 4️⃣ 우리 서비스 JWT 발급
        String accessToken =
                tokenProvider.generateToken(user, Duration.ofHours(1));

        String refreshToken =
                tokenProvider.generateToken(user, Duration.ofDays(14));

        // 5️⃣ Refresh Token 저장
        saveRefreshToken(user.getId(), refreshToken);

        return new LoginResponse(accessToken, refreshToken);
    }

    private void saveRefreshToken(Long userId, String newRefreshToken) {
        RefreshToken refreshToken =
                refreshTokenRepository.findByUserId(userId)
                        .map(entity -> entity.update(newRefreshToken))
                        .orElse(new RefreshToken(userId, newRefreshToken));

        refreshTokenRepository.save(refreshToken);
    }
}
