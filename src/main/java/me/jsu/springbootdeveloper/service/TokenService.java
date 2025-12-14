package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.domain.User;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public String createNewAccessToken(String refreshToken) {  // 유효한 리프레시 토큰을 받아서 새로운 엑세스 토큰을 만들어 반환하는 메서드
        // 토큰 유효성 검사에 실패하면 예외 발생
        if(!tokenProvider.validToken(refreshToken)) {
            throw new IllegalArgumentException("Unexpected token");
        }

        // 실무에선 유저id가 없는 경우에 대한 런타임 에러 처리를 해야함
        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();  // 리프레시 토큰에 등록된 유저 id 검색
        User user = userService.findById(userId);  // 리프레시 토큰에서 가져온 유저 id로 유저 정보를 검색

        return tokenProvider.generateToken(user, Duration.ofHours(2));  // 가져온 유저 정보를 바탕으로 새로운 엑세스 토큰 발급
    }
}
