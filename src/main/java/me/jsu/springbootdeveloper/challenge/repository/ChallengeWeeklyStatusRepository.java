// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 챌린지 주간 상태의 값 중, 그 주의 총 인증 횟수를 유저 id와 챌린지 id로 검색하여 페이지 크기만큼 내림차순으로 반환한다.
    @Query("""
        select ws.successCount
        from ChallengeWeeklyStatus ws
        where ws.user.id = :userId
          and ws.challenge.id = :challengeId
        order by ws.weekStartDate desc
    """)
    List<Integer> findRecentSuccessCounts(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            Pageable pageable
    );

    // 유저 id와 챌린지 id로 이번주 상태를 삭제한다.
    void deleteByUser_IdAndChallenge_Id(Long userId, Long challengeId);
}
