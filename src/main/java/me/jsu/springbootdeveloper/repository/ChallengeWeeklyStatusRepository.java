// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.ChallengeWeeklyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/*
    챌린지의 주간 인증 횟수를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 ChallengeWeeklyStatus, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeWeeklyStatusRepository extends JpaRepository<ChallengeWeeklyStatus, Long> {
}
