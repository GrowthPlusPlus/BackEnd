// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.dto.RankingResponse;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
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
        where cp.user.id = :userId
          and cp.status = 'IN_PROGRESS'
    """)
    List<ChallengeParticipant> findByUser_IdForCpInProgress(@Param("userId") Long userId);  // 유저 id로 유저가 참여한 진행중인 챌린지들을 검색한다.

    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.challenge
        where cp.user.id = :userId
    """)
    List<ChallengeParticipant> findWithChallengeByUserId(@Param("userId") Long userId);  // 유저 id로 유저가 참여한 챌린지들을 검색한다. 챌린지 자체 정보도 함께 가져온다.

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
        order by cp.joinDate desc
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
              and (:nickname is null or u.nickname like :nickname)
        """,
            countQuery = """
            select count(cp)
            from ChallengeParticipant cp
            join cp.user u
            where cp.challenge.id = :challengeId
              and (:nickname is null or u.nickname like :nickname)
        """
    )
    Page<ChallengeParticipant> findMembersWithUserAndPicture(
            @Param("challengeId") Long challengeId,
            @Param("nickname") String nickname,
            Pageable pageable
    );

    // 특정 유저의 챌린지 성공 일수를 여러 챌린지 id로 조회하기 위해 사용하는 쿼리문이다.
    @Query("""
        select cp.user.id, cp.challenge.id, cp.totalSuccessDays
        from ChallengeParticipant cp
        where cp.user.id in :userIds
          and cp.challenge.id in :challengeIds
    """)
    List<Object[]> findSuccessDays(
            @Param("userIds") List<Long> userIds,
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

    boolean existsByChallenge_IdAndUser_Id(Long challengeId, Long userId);

    // 상위 랭커를 조회하는 쿼리문이다. 등수는 서비스에서 다시 계산한다.
    @Query("""
        select new me.jsu.springbootdeveloper.challenge.dto.RankingResponse(
            u.id,
            u.nickname,
            p.imageUrl,
            cp.rankingScore,
            0
        )
        from ChallengeParticipant cp
        join cp.user u
        left join u.userProfilePicture p
        where cp.challenge.id = :challengeId
        order by cp.rankingScore desc
    """)
    List<RankingResponse> findTopRankers(
            @Param("challengeId") Long challengeId,
            Pageable pageable
    );

    /*
        사용자의 이름, 프로필 사진, 스코어를 한 번에 조회하는 쿼리문이다.
     */
    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.user u
        left join fetch u.userProfilePicture p
        where cp.challenge.id = :challengeId
        and u.id = :userId
    """)
    Optional<ChallengeParticipant> findMyParticipantWithFetch(
            @Param("challengeId") Long challengeId,
            @Param("userId") Long userId
    );

    // 해당 사용자의 등수를 검색한다.
    @Query("""
        select count(cp)
        from ChallengeParticipant cp
        where cp.challenge.id = :challengeId
        and cp.rankingScore > :score
    """)
    long countHigherScore(
            @Param("challengeId") Long challengeId,
            @Param("score") double score
    );

    @Query("""
        select cp
        from ChallengeParticipant cp
        join fetch cp.user u
        where cp.id > :lastId
          and cp.status = 'IN_PROGRESS'
        order by cp.id asc
    """)
    List<ChallengeParticipant> findInProgressParticipantsAfterId(
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}
