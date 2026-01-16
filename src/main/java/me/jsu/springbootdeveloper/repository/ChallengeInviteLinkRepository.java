// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.ChallengeInviteLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeInviteLinkRepository extends JpaRepository<ChallengeInviteLink, Long> {
    List<ChallengeInviteLink> findAllByUser_Id(Long userId);  // 해당 유저가 생성한 모든 초대 링크를 조회한다.
    List<ChallengeInviteLink> findAllByUser_IdAndActiveTrue(Long userId);  // 해당 유저가 생성한 모든 유효 초대 링크를 조회한다.
}
