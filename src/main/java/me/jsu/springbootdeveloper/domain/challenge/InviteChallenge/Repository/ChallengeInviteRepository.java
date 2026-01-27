package me.jsu.springbootdeveloper.domain.challenge.InviteChallenge.Repository;

import me.jsu.springbootdeveloper.domain.challenge.InviteChallenge.Entity.ChallengeInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeInviteRepository extends JpaRepository<ChallengeInvite, Long> {

    //특정 챌린지에 초대된 사람들 목록 조회
    List<ChallengeInvite> findByChallenge_Id(Long challengeId);
    //특정 유저가 받은 초대 목록 조회
    List<ChallengeInvite> findByInvitee_Id(Long inviteeId);
    //중복 초대 방지를 위한 확인
    boolean existsByChallenge_IdAndInvitee_Id(Long challengeId, Long inviteeId);
}
