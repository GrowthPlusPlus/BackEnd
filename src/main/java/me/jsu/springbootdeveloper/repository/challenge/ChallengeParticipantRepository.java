// 정성우

package me.jsu.springbootdeveloper.repository.challenge;

import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.challenge.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    List<ChallengeParticipant> findWithChallengeByUserId(@Param("userId") Long userId);  // 유저 id로 유저가 참여한 챌린지들을 검색한다.

    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.user
        where cp.challenge.id = :challengeId
    """)
    List<ChallengeParticipant> findWithUserByChallengeId(@Param("challengeId") Long challengeId);  // 챌린지 id로 챌린지에 참여한 유저들을 검색한다.

    // 유저 id로 유저가 참여한 챌린지 중, 현재 진행중인 챌린지들을 검색한다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.challenge c
        where cp.user.id = :userId
          and cp.status = 'IN_PROGRESS'
    """)
    List<ChallengeParticipant> findInProgressWithChallengeByUserId(@Param("userId") Long userId);

    // 유저 id와 챌린지 상태로 챌린지 참여 DB와 챌린지 자체 DB를 함께 검색한다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.challenge
        where cp.user.id = :userId
          and cp.status = :status
    """)
    List<ChallengeParticipant> findStatusWithChallengeByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") Status status
    );

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
            @Param("challengeId") Long challengeId,
            @Param("ownerId") Long ownerId
    );

    // 챌린지 id와 유저 id로 챌린지 참여 DB를 검색한다. 챌린지 참여 DB 정보만 필요하므로 쿼리문은 사용하지 않는다.
    Optional<ChallengeParticipant> findByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    // 챌린지 id로 해당 챌린지에 참여한 유저의 id, 이름, 프로필 사진 링크를 가져오는 쿼리문이다. (닉네임은 필요하다면 사용한다.)
    @Query(
            value = """
            select cp
            from ChallengeParticipant cp
            join fetch cp.user u
            left join fetch u.userProfilePicture upp
            where cp.challenge.id = :challengeId
              and (:nickname is null or u.nickname like concat('%', :nickname, '%'))
        """,
            countQuery = """
            select count(cp)
            from ChallengeParticipant cp
            join cp.user u
            where cp.challenge.id = :challengeId
              and (:nickname is null or u.nickname like concat('%', :nickname, '%'))
        """
    )
    Page<ChallengeParticipant> findMembersWithUserAndPicture(
            @Param("challengeId") Long challengeId,
            @Param("nickname") String nickname,
            Pageable pageable
    );

    // 특정 유저의 챌린지 성공 일수를 여러 챌린지 id로 조회하기 위해 사용하는 쿼리문이다.
    @Query("""
        select cp.challenge.id, cp.totalSuccessDays
        from ChallengeParticipant cp
        where cp.user.id = :userId
          and cp.challenge.id in :challengeIds
    """)
    List<Object[]> findSuccessDays(
            @Param("userId") Long userId,
            @Param("challengeIds") List<Long> challengeIds
    );

    // 현재 진행중인 챌린지들을 검색한다. 커서 기반으로 검색한다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        where cp.status = 'IN_PROGRESS'
          and cp.id > :lastId
        order by cp.id asc
    """)
    List<ChallengeParticipant> findAllInProgressAfterId(@Param("lastId") Long lastId, Pageable pageable);

    // 현재 진행중이면서 종료일인 챌린지들을 검색한다. batch 사이즈 단위로 조사한다. endDate < today 이므로 종료 당일도 챌린지 수행이 가능하다.
    @Query("""
        select cp
        from ChallengeParticipant cp
        join cp.challenge c
        where cp.status = 'IN_PROGRESS'
          and c.endDate < :today
          and cp.id > :lastId
        order by cp.id asc
    """)
    List<ChallengeParticipant> findEndedInProgressAfterId(@Param("lastId") Long lastId, @Param("today") LocalDate today, Pageable pageable);
}
