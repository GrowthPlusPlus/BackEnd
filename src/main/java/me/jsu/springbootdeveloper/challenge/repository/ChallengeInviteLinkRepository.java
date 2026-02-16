// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeInviteLinkRepository extends JpaRepository<ChallengeInviteLink, Long> {
    /*
        해당 유저가 생성한 모든 초대 링크를 조회한다.
     */
    List<ChallengeInviteLink> findAllByUser_Id(Long userId);

    /*
        해당 유저가 생성한 모든 활성화된 초대 링크를 조회한다.
     */
    List<ChallengeInviteLink> findAllByUser_IdAndActiveTrue(Long userId);

    /*
        특정 챌린지의 활성화된 초대 링크를 조회한다.
     */
    List<ChallengeInviteLink> findAllByChallenge_IdAndActiveTrue(Long challengeId);
    
    /*
        초대 코드로 초대 링크를 조회한다.
     */
    Optional<ChallengeInviteLink> findByInviteCode(String inviteCode);

    /*
        특정 챌린지의 유효한 초대 링크를 조회한다.
        유효한 링크: active = true이고, expiresAt이 아직 지나지 않은 링크
     */
    @Query("""
        select link
        from ChallengeInviteLink link
        where link.challenge.id = :challengeId
          and link.active = true
          and link.expiresAt > :now
        order by link.createdAt desc
    """)
    List<ChallengeInviteLink> findValidLinksByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("now") LocalDateTime now
    );

    /*
        특정 챌린지의 유효한 초대 링크 중 첫 번째 링크를 조회한다.
        유효한 링크: active = true이고, expiresAt이 아직 지나지 않은 링크
        createdAt 내림차순으로 정렬된 결과 중 첫 번째를 반환한다.
     */
    @Query("""
        select link
        from ChallengeInviteLink link
        where link.challenge.id = :challengeId
          and link.active = true
          and link.expiresAt > :now
        order by link.createdAt desc
    """)
    Optional<ChallengeInviteLink> findFirstValidLinkByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("now") LocalDateTime now
    );

    /*
        유저 id와 챌린지 id로 초대 링크를 삭제한다.
     */
    void deleteByUser_IdAndChallenge_Id(Long userId, Long challengeId);
}
