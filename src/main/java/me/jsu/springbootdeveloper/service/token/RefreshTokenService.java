// 정성우

package me.jsu.springbootdeveloper.service.token;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.RefreshToken;
import me.jsu.springbootdeveloper.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

/*
    리프레시 토큰에 관한 DB 처리를 위한 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    /*
        리프레시 토큰으로 서버에 저장된 리프레시 토큰을 찾는 함수이다.
     */
    public RefreshToken findByRefreshToken(String refreshToken) {
        return refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("REFRESH_TOKEN_NOT_FOUND"));
    }

    public RefreshToken save(Long userId, String refreshToken) {
        return refreshTokenRepository.save(new RefreshToken(userId, refreshToken));
    }

    public void delete(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }
}
