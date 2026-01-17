// 정성우

package me.jsu.springbootdeveloper.repository.challenge;

import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // 유저 id로 유저가 참여한 챌린지 중, 현재 진행중인 챌린지들을 검색한다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.challenge c
        where cp.user.id = :userId
          and cp.status = 'IN_PROGRESS'
    """)
    List<ChallengeParticipant> findInProgressWithChallengeByUserId(Long userId);

    // 챌린지 id와 오너 id로 해당 챌린지의 참가자를 참가 순서대로 오너 제외하고 반환한다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.user
        where cp.challenge.id = :challengeId
          and cp.user.id <> :ownerId
        order by cp.createdAt asc
    """)
    List<ChallengeParticipant> findParticipantsExceptOwner(
            Long challengeId,
            Long ownerId
    );
}
