package me.jsu.springbootdeveloper.token.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.token.domain.RefreshToken;
import me.jsu.springbootdeveloper.token.exception.InvalidTokenException;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.token.dto.TokenPair;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.Duration;

/*
    해당 클래스는 토큰 로테이션을 구현한다.
    리프레시 토큰을 받아 새로운 리프레시 토큰과 엑세스 토큰을 생성하여 발급한다.
 */
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    /*
        유효한 리프레시 토큰을 받아서 새로운 엑세스 토큰과 리프레시 토큰을 만들어 반환하는 함수이다.
     */
    public TokenPair rotateRefreshToken(String refreshToken) {
        // 리프레시 토큰에 대한 유효성 검사에 실패하면 예외가 발생한다.
        if(!tokenProvider.validToken(refreshToken)) {
            throw new InvalidTokenException("INVALID_REFRESH_TOKEN");
        }

        // 유저에게 받은 리프레시 토큰으로 서버에 저장된 리프레스 토큰 검색한다.
        RefreshToken storedToken = refreshTokenService.findByRefreshToken(refreshToken);
        User user = storedToken.getUser();  // 리프레시 토큰에 등록된 유저 정보를 검색한다.

        refreshTokenService.delete(storedToken);  // 기존 DB에 저장된 리프레시 토큰을 삭제한다.

        /*
            새로운 access token, refresh token을 생성한다.
            또한 리프레시 토큰은 DB에 저장한다.
         */
        String newAccessToken = tokenProvider.generateToken(user, Duration.ofHours(1));
        String newRefreshToken = tokenProvider.generateToken(user, Duration.ofDays(30));
        refreshTokenService.save(user, newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);  // 생성한 두 토큰을 반환한다.
    }
}
