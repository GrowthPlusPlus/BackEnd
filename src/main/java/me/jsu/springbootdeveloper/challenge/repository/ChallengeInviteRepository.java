package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeInviteRepository extends JpaRepository<ChallengeInvite, Long> {

    //특정 챌린지에 초대된 사람들 목록 조회
    List<ChallengeInvite> findByChallenge_Id(Long challengeId);
    /*
        특정 유저가 받은 초대 목록 중 만료되지 않은 챌린지를 조회한다.
        챌린지가 만료되지 않았다는 것은 endDate가 오늘 날짜보다 크거나 같은 경우를 의미한다.
     */
    @Query("""
            select c.challenge
            from ChallengeInvite c
            where c.invitee.id = :inviteeId
                and c.challenge.endDate >= :today
            """)
    List<Challenge> findActiveChallengesByInvitee_Id(
            @Param("inviteeId") Long inviteeId,
            @Param("today") LocalDate today
    );

    // 중복 초대 방지를 위한 확인을 위해 사용한다. 이미 초대된 사람들의 id를 한 번에 조회한다.
    @Query("""
        select ci.invitee.id
        from ChallengeInvite ci
        where ci.challenge.id = :challengeId
          and ci.invitee.id in :inviteeIds
    """)
    List<Long> findInvitedUserIdsIn(
            @Param("challengeId") Long challengeId,
            @Param("inviteeIds") List<Long> inviteeIds
    );

    // 챌린지 id와 초대된 사람 id로 검색한다.
    Optional<ChallengeInvite> findByChallenge_IdAndInvitee_Id(Long challengeId, Long inviteeId);

    /*
        특정 유저가 받은 초대 목록 중 만료되지 않은 챌린지의 초대 정보를 조회한다.
        초대자 정보와 챌린지 정보를 함께 가져온다.
        챌린지가 만료되지 않았다는 것은 endDate가 오늘 날짜보다 크거나 같은 경우를 의미한다.
     */
    @Query("""
            select ci
            from ChallengeInvite ci
            join fetch ci.inviter
            join fetch ci.challenge
            where ci.invitee.id = :inviteeId
                and ci.challenge.endDate >= :today
                and ci.status = 'INVITED'
            order by ci.createdAt desc
            """)
    List<ChallengeInvite> findActiveInvitesByInvitee_Id(
            @Param("inviteeId") Long inviteeId,
            @Param("today") LocalDate today
    );

}
