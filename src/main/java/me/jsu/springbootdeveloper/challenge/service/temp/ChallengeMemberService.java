// 정성우

package me.jsu.springbootdeveloper.challenge.service.temp;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeMemberResponse;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeOwnerCannotLeaveException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.*;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
    챌린지에 참여한 맴버를 관리하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class ChallengeMemberService {
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeInviteLinkRepository challengeInviteLinkRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;
    private final ChallengeInviteRepository challengeInviteRepository;
    private final UserStatisticsService userStatisticsService;
    private final UserService userService;

    /*
        챌린지에 참여한 유저를 검색하는 함수이다.
     */
    public Page<ChallengeMemberResponse> getMembers(String email, Long challengeId, int page, String nickname) {
        User user = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 인증글 개수
                Sort.by(Sort.Order.asc("user.nickname"))  // 닉네임을 기준으로 오름차순 검색한다.
        );

        // 방장이 챌린지 참여자인지 확인한다.
        challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        // % 처리를 리포지토리가 아니라 서비스에서 한다.
        String keyword = (nickname == null || nickname.isBlank()) ? null : "%" + nickname + "%";
        Page<ChallengeParticipant> challengeParticipants = challengeParticipantRepository.findMembersWithUserAndPicture(challengeId, keyword, pageable);

        // 검색한 맴버를 ChallengeMemberResponse에 매핑하여 반환한다.
        return challengeParticipants.map(challengeParticipant -> {
            User userEntity = challengeParticipant.getUser();
            UserProfilePicture picture = userEntity.getUserProfilePicture();

            return new ChallengeMemberResponse(
                    userEntity.getId(),
                    userEntity.getNickname(),
                    picture != null ? picture.getImageUrl() : null  // 프로필 사진이 null값인지 아닌지 검사한다.
            );
        });
    }

    /*
        챌린지의 방장을 위임하기 위한 함수이다.
     */
    @Transactional
    public void delegateOwner(String email, Long challengeId, Long targetUserId) {
        User user = userService.findByEmail(email);

        // 챌린지를 조회한다.
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 방장 권한을 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 위임 대상자가 참여자가 맞는지 검증하고 검색한다.
        ChallengeParticipant targetParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(targetUserId, challengeId);

        // 방장을 변경한다.
        challenge.updateUser(targetParticipant.getUser());

        // 챌린지에서 나간다.
        leaveChallengeInternal(user.getId(), challengeId);
    }

    /*
        컨트롤러와 연결된 챌린지에서 유저가 나갈 수 있도록 하는 함수이다.
        컨트롤러-서비스 구조의 일관성을 위해 실제 처리 함수랑 분리한다.
     */
    @Transactional
    public void leaveChallenge(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        leaveChallengeInternal(user.getId(), challengeId);
    }

    /*
        챌린지에서 유저를 추방하기 위한 함수이다.
     */
    @Transactional
    public void kickMember(String email, Long challengeId, Long targetUserId) {
        User user = userService.findByEmail(email);

        // 추방하려는 사용자가 방장인지 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 자기 자신을 추방하는 것을 방지한다.
        if (user.getId().equals(targetUserId)) {
            throw new NotAllowedChallengeActionException("CANNOT_KICK_OWNER");
        }

        // 대상 유저를 추방한다.
        leaveChallengeInternal(targetUserId, challengeId);

        // 추방하려는 유저의 챌린지 초대 튜플을 검색한다.
        ChallengeInvite challengeInvite = challengeInviteRepository.findByChallenge_IdAndInvitee_Id(challengeId, targetUserId)
                .orElseThrow(() -> new ChallengeInviteNotFoundException("초대 정보가 존재하지 않습니다."));

        // 초대 상태가 됐던 유저의 챌린지 초대 DB를 삭제한다.
        challengeInviteRepository.delete(challengeInvite);
    }

    /*
        챌린지에서 유저가 나갈 수 있도록 하는 함수이다.
        방장은 나가기가 불가능하며, 관련된 종속 데이터를 정리한다.
        서비스 내부에서 사용하기 위해 유저 id로 삭제한다.
     */
    public void leaveChallengeInternal(Long userId, Long challengeId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 해당 챌린지 참여자를 검색한다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);

        // 방장 여부를 판단한다.
        boolean ChallengeOwner = isOwner(userId, challengeId);

        // 방장은 나가기가 불가하다는 예외를 던진다.
        if (ChallengeOwner) {
            throw new ChallengeOwnerCannotLeaveException("CHALLENGE_OWNER_CANNOT_LEAVE");
        }

        /*
            챌린지에서 나가면 의미 없는 종속 DB 값들을 유저 id와 챌린지 id로 삭제한다.
            챌린지 초대 링크, 챌린지 위크 스테이터스 DB가 이에 해당한다.
         */
        challengeInviteLinkRepository.deleteByUser_IdAndChallenge_Id(userId, challengeId);
        challengeWeeklyStatusRepository.deleteByUser_IdAndChallenge_Id(userId, challengeId);

        // 참여자 정보를 실제로 삭제한다.
        challengeParticipantRepository.delete(challengeParticipant);

        // 챌린지의 전체 참여자 수를 감소시킨다.
        challenge.updateLeave();

        // 유저가 참여한 총 챌린지 개수를 감소시킨다.
        UserStatistics userStatistics = userStatisticsService.findByUser_IdStatistics(userId);
        userStatistics.decreaseChallengeCount();
    }

    /*
        해당 챌린지에 대해 유저가 방장인지 검사한다.
     */
    public boolean isOwner(Long userId, Long challengeId) {
        return challengeRepository.existsByIdAndUser_Id(challengeId, userId);
    }

    /*
        isOwner에서 검사한 값을 바탕으로 해당 사용자가 방장인지 검사한다.
     */
    public void validateOwner(boolean challengeOwner) {
        if (!challengeOwner) {
            throw new NotAllowedChallengeActionException("NOT_CHALLENGE_OWNER");
        }
    }
}
