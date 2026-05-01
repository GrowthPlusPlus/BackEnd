// 정성우

package me.jsu.springbootdeveloper.token.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.token.domain.RefreshToken;
import me.jsu.springbootdeveloper.token.exception.TokenNotFoundException;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.token.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /*
        리프레시 토큰을 저장하는 함수이다.
     */
    public RefreshToken save(User user, String refreshToken) {
        return refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .build()
        );
    }

    /*
        리프레시 토큰 객체로 리프레시 토큰을 삭제한다.
     */
    public void delete(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    /*
        유저 id로 리프레시 토큰을 삭제한다.
     */
    public void deleteByUser_Id(Long userId) {
        refreshTokenRepository.deleteByUser_Id(userId);
    }

    /*
        OAuth 로그인 전용으로 토큰이 있으면 update, 없으면 create 한다.
     */
    @Transactional
    public void saveForOAuth(User user, String newRefreshToken) {
        refreshTokenRepository.findByUser_Id(user.getId())  // 리프레시 토큰 DB에서 유저 정보를 검색한다.
                .ifPresentOrElse(
                        token -> token.update(newRefreshToken),  // 재로그인이라면 리프레시 토큰을 업데이트 한다.
                        () -> refreshTokenRepository.save(  // 최초 로그인이라면 새로 작성한 후, DB에 저장한다.
                                RefreshToken.builder()
                                        .user(user)
                                        .refreshToken(newRefreshToken)
                                        .build()
                        )
                );
    }
}
