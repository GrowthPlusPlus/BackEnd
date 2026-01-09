package me.jsu.springbootdeveloper.service.token;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.domain.RefreshToken;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.TokenPair;
import me.jsu.springbootdeveloper.service.UserService;
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

        // 실무에선 유저id가 없는 경우에 대한 런타임 에러 처리를 해야함 -> UserService에서 구현

        // 유저에게 받은 리프레시 토큰으로 서버에 저장된 리프레스 토큰 검색한다.
        RefreshToken storedToken = refreshTokenService.findByRefreshToken(refreshToken);
        Long userId = storedToken.getUserId();  // 리프레시 토큰에 등록된 유저 id를 추출한다.
        User user = userService.findById(userId);  // 리프레시 토큰에서 가져온 유저 id로 유저 정보를 검색한다.

        refreshTokenService.delete(storedToken);  // 기존 DB에 저장된 리프레시 토큰을 삭제한다.

        /*
            새로운 access token, refresh token을 생성한다.
            또한 리프레시 토큰은 DB에 저장한다.
         */
        String newAccessToken = tokenProvider.generateToken(user, Duration.ofHours(1));
        String newRefreshToken = tokenProvider.generateToken(user, Duration.ofDays(30));
        refreshTokenService.save(userId, newRefreshToken);

        return new TokenPair(newAccessToken, newRefreshToken);  // 생성한 두 토큰을 반환한다.
    }
}
