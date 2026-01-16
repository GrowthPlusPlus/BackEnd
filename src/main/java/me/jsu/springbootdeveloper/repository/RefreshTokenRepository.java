// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    리프레시 토큰을 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 refreshToken, 엔티티의 pk 타입은 Long이다.
    findByAll 검색 방식 이외에도 유저 정보, 리프레시 토큰 값으로 조회가 가능하다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser_Id(Long userId);
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
