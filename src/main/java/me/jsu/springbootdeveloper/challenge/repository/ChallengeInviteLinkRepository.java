// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeInviteLinkRepository extends JpaRepository<ChallengeInviteLink, Long> {
    List<ChallengeInviteLink> findAllByUser_Id(Long userId);  // 해당 유저가 생성한 모든 초대 링크를 조회한다.
    List<ChallengeInviteLink> findAllByUser_IdAndActiveTrue(Long userId);  // 해당 유저가 생성한 모든 유효 초대 링크를 조회한다.

    // 유저 id와 챌린지 id로 초대 링크를 삭제한다.
    void deleteByUser_IdAndChallenge_Id(Long userId, Long challengeId);
}
