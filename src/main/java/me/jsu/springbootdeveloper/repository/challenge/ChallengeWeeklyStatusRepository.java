// 정성우

package me.jsu.springbootdeveloper.repository.challenge;

import me.jsu.springbootdeveloper.domain.challenge.ChallengeWeeklyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
    챌린지의 주간 인증 횟수를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 ChallengeWeeklyStatus, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeWeeklyStatusRepository extends JpaRepository<ChallengeWeeklyStatus, Long> {
    Optional<ChallengeWeeklyStatus> findByUser_IdAndChallenge_IdAndWeekStartDate(   // 이번 주의 챌린지 상태 엔트리를 조회한다.
            Long userId,
            Long challengeId,
            LocalDate weekStartDate
    );

    // 이번 주의 챌린지 상태 엔트리들을 유저 id로 조회한다.
    List<ChallengeWeeklyStatus> findByUser_IdAndWeekStartDate(
            Long userId,
            LocalDate weekStartDate
    );

    // 유저 id와 챌린지 id로 이번주 상태를 삭제한다.
    void deleteByUser_IdAndChallenge_Id(Long userId, Long challengeId);
}
