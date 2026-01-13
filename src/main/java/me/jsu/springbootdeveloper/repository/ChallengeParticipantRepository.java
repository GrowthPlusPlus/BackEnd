// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지 참여자를 관리하는 DB로, 테이블에 매핑되는 엔티티 타입은 ChallengeParticipant, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.challenge
        where cp.user.id = :userId
    """)
    List<ChallengeParticipant> findWithChallengeByUserId(Long userId);  // 유저 id로 유저가 참여한 챌린지들을 검색한다.

    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.user
        where cp.challenge.id = :challengeId
    """)
    List<ChallengeParticipant> findWithUserByChallengeId(Long challengeId);  // 챌린지 id로 챌린지에 참여한 유저들을 검색한다.
}
