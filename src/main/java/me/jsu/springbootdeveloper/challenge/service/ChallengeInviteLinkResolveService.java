// 정하늘

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import me.jsu.springbootdeveloper.challenge.dto.invite.ChallengeInviteLinkResolveResponse;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteLinkExpiredException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteLinkNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotInProgressException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteLinkRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/*
    초대 링크 코드를 앱에서 해석해 챌린지 상세 화면으로 이동할 challengeId를 반환하는 서비스이다.
    링크로 들어온 사용자는 ChallengeInvite를 만들지 않으며,
    참여는 챌린지 상세 화면의 participate API로 처리한다.
 */
@RequiredArgsConstructor
@Service
public class ChallengeInviteLinkResolveService {
    private final ChallengeInviteLinkRepository challengeInviteLinkRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final UserService userService;

    /*
        inviteCode를 해석해 해당 챌린지 ID를 반환한다.
        ChallengeInvite는 생성하지 않는다.

        @param email 로그인한 유저 이메일
        @param inviteCode 딥링크 또는 랜딩 페이지 URL의 초대 코드
        @return 챌린지 상세 화면 이동에 필요한 challengeId
     */
    @Transactional(readOnly = true)
    public ChallengeInviteLinkResolveResponse resolveInviteLink(String email, String inviteCode) {
        User user = userService.findByEmail(email);
        ChallengeInviteLink inviteLink = findValidInviteLink(inviteCode);
        Challenge challenge = inviteLink.getChallenge();
        LocalDate today = LocalDate.now();

        validateChallengeInProgress(challenge, today);

        boolean alreadyParticipant = challengeParticipantRepository.existsByUser_IdAndChallenge_Id(
                user.getId(),
                challenge.getId()
        );

        if (!alreadyParticipant && challenge.checkIfOverMaxParticipantNumber()) {
            throw new NotAllowedChallengeActionException("CHALLENGE_FULL");
        }

        return new ChallengeInviteLinkResolveResponse(challenge.getId(), alreadyParticipant);
    }

    private ChallengeInviteLink findValidInviteLink(String inviteCode) {
        String normalizedInviteCode = inviteCode == null ? "" : inviteCode.trim();

        ChallengeInviteLink inviteLink = challengeInviteLinkRepository.findByInviteCode(normalizedInviteCode)
                .orElseThrow(() -> new ChallengeInviteLinkNotFoundException("INVITE_LINK_NOT_FOUND"));

        if (!inviteLink.canBeUsed()) {
            throw new ChallengeInviteLinkExpiredException("INVITE_LINK_EXPIRED");
        }

        return inviteLink;
    }

    private void validateChallengeInProgress(Challenge challenge, LocalDate today) {
        if (challenge.getEndDate().isBefore(today)) {
            throw new ChallengeNotInProgressException("CHALLENGE_NOT_IN_PROGRESS");
        }
    }
}
