// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

/*
    챌린지를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Challenge, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
}
