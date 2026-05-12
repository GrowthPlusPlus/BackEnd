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
    // 이번 주의 챌린지 상태 엔트리를 하나 조회한다.
    Optional<ChallengeWeeklyStatus> findByUser_IdAndChallenge_IdAndWeekStartDate(
            Long userId,
            Long challengeId,
            LocalDate weekStartDate
    );

    // 이번주의 챌린지 상태 엔트리를 한 번에 조회한다.
    @Query("""
        select cws
        from ChallengeWeeklyStatus cws
        where cws.weekStartDate = :weekStart
          and cws.user.id in :userIds
          and cws.challenge.id in :challengeIds
    """)
    List<ChallengeWeeklyStatus> findAllByUsersAndChallenges(
            @Param("userIds") List<Long> userIds,
            @Param("challengeIds") List<Long> challengeIds,
            @Param("weekStart") LocalDate weekStart
    );

    // 이번 주의 챌린지 상태 엔트리들을 유저 id로 조회한다.
    @Query("""
    select cws
    from ChallengeWeeklyStatus cws
    join fetch cws.challenge ch
    where cws.user.id = :userId
      and cws.weekStartDate = :weekStartDate
""")
    List<ChallengeWeeklyStatus> findByUser_IdAndWeekStartDate(
            @Param("userId") Long userId,
            @Param("weekStartDate") LocalDate weekStartDate
    );

    // 챌린지 주간 상태의 값 중, 그 주의 총 인증 횟수를 구하기 위해 유저 id 리스트로 검색한다. cutoffDate로 너무 오래된 주차까지 검색하지 않도록 한다.
    @Query("""
        select ws.user.id,
               ws.challenge.id,
               ws.successCount
        from ChallengeWeeklyStatus ws
        where ws.user.id in :userIds
            and ws.weekStartDate >= :cutoffDate
        order by ws.user.id,
                 ws.challenge.id,
                 ws.weekStartDate desc
    """)
    List<Object[]> findRecentSuccessCountsForUsers(
            @Param("userIds") List<Long> userIds,
            @Param("cutoffDate") LocalDate cutoffDate
    );

    // 유저 id와 챌린지 id로 이번주 상태를 삭제한다.
    void deleteByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    /*
        start ~ end 기간을 한 달로 잡아서 해당 기간동안의 주간 챌린지 성공 횟수를 조회하는 쿼리문이다.
        challenge_participant값이 있어야 집계에 포함한다.
     */
    @Query("""
        select w.weekStartDate, sum(w.successCount)
        from ChallengeWeeklyStatus w
        where w.user.id = :userId
          and w.weekStartDate between :start and :end
          and w.challenge is not null
          and exists (
            select 1
            from ChallengeParticipant cp
            where cp.user.id = :userId
                and cp.challenge.id = w.challenge.id
          )
        group by w.weekStartDate
        order by w.weekStartDate asc
    """)
    List<Object[]> sumWeeklyInRange(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
