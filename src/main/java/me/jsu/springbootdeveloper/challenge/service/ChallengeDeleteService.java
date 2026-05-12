// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeDailyLogRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/*
    챌린지와 관련된 값을 삭제하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class ChallengeDeleteService {
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeDailyLogRepository challengeDailyLogRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final UserStatisticsService userStatisticsService;

    /*
        유저가 회원탈퇴를 하는 경우, 해당 유저가 오너인 챌린지가 삭제되지 않도록 다른 유저에게 위임한다.
        방장 외에 유저가 없다면 챌린지를 삭제한다.
     */
    public void handleOwnerWithdrawal(Long userId) {
        List<Long> toDelete = new ArrayList<>();  // 삭제할 챌린지들의 id를 모은 배열이다.
        List<Challenge> challenges = challengeQueryService.findByUser_Id(userId);  // 현재 관리하는 챌린지들을 검색한다.

        if (challenges.isEmpty()) {  // 관리하는 챌린지가 없다면 리턴한다.
            return;
        }

        // 관리하는 챌린지의 id들을 따로 map으로 만든다.
        List<Long> challengeIds = challenges.stream()
                .map(Challenge::getId)
                .toList();

        // 챌린지 오너를 제외한 챌린지 참가자들을 검색한다.
        List<ChallengeParticipant> allParticipants = challengeParticipantRepository.findParticipantsExceptOwnerForChallenges(challengeIds, userId, Status.IN_PROGRESS);

        // 검색한 참가자들의 정보를 challengeId를 기준으로 그룹화한다.
        Map<Long, List<ChallengeParticipant>> grouped = allParticipants.stream()
                        .collect(Collectors.groupingBy(cp -> cp.getChallenge().getId()));

        // 챌린지별로 위임할 참가자들을 선택한다.
        for (Challenge challenge : challenges) {
            // 위임할 참가자를 검색한다.
            List<ChallengeParticipant> participants = grouped.getOrDefault(challenge.getId(), List.of());

            if (participants.isEmpty()) {  // 위임할 참가자가 없다면 챌린지를 삭제 목록에 추가한다.
                toDelete.add(challenge.getId());
            }
            else {  // 스코어가 가장 높은 사람에게 위임한다.
                User newOwner = selectNextOwner(participants);

                challenge.updateUser(newOwner);
            }
        }

        // 반복이 끝난 후 챌린지를 삭제한다.
        for (Long id : toDelete) {
            deleteChallengeInternal(id);
        }
    }

    /*
        챌린지를 실제로 삭제하는 서비스 클래스이다. 내부적으로 재사용하기 위해 분리하여 사용한다.
        데일리 로그, 인증글은 FK만 null 처리하고, 나머지 종속된 DB값들은 cascade 옵션으로 자동 삭제한다.
     */
    public void deleteChallengeInternal(Long challengeId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 삭제 전에 잔디의 재집계를 위해 챌린지에 참여한 유저 id를 저장한다.
        List<Long> userIds = challengeQueryService.findUserIdsByChallengeId(challengeId);

        // 데일리 로그, 인증글의 fK값을 null 처리한다.
        challengeDailyLogRepository.detachChallenge(challenge);
        postRepository.detachChallenge(challenge);  // 순환참조 방지를 위해 서비스를 사용하지 않고 직접 사용한다.

        // 챌린지를 삭제한다. 나머지 종속된 값들은 cascade REMOVE 옵션으로 자동 삭제된다.
        challengeRepository.delete(challenge);

        // 삭제된 챌린지에 참여했던 유저들의 잔디를 재집계 한다.
        for (Long userId : userIds) {
            userStatisticsService.researchLog(userId, LocalDate.now(ZoneId.of("Asia/Seoul")));
        }
    }

    /*
        챌린지를 삭제하는 서비스 클래스이다.
        컨트롤러-서비스 구조를 유지하기 위해 사용한다.
     */
    @Transactional
    public void deleteChallenge(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        // 방장 권한을 검증한다.
        if(!challengeRepository.existsByIdAndUser_Id(challengeId, user.getId())) {
            throw new NotAllowedChallengeActionException("NOT_CHALLENGE_OWNER");
        }

        // 챌린지를 삭제한다.
        deleteChallengeInternal(challengeId);
    }

    /*
        모든 챌린지에서 유저가 나올때 사용하는 함수이다. 회원탈퇴 시에 사용된다.
        해당 함수는 방장 위임이 끝난 뒤에 사용해야 한다.
     */
    public void deleteParticipant(List<Long> userIds) {
        // 유저 아이디로 해당 유저가 참가한 챌린지들을 검색한다.
        List<ChallengeParticipant> participants = challengeQueryService.findAllByUserIds(userIds);
        if (participants.isEmpty()) {  // 참여한 챌린지가 없다면 리턴한다.
            return;
        }

        challengeParticipantRepository.deleteAllInBatch(participants);  // 챌린지 참여에서 삭제한다.

        for (ChallengeParticipant challengeParticipant : participants) {
            updateChallengeParticipantCount(challengeParticipant.getChallenge());  // 챌린지 참여자 수를 감소시킨다.
        }
    }

    /*
        챌린지 데일리 로그를 실제로 삭제한다.
        인증글을 삭제하는 경우에 사용한다.
     */
    public void deleteLog(ChallengeDailyLog challengeDailyLog) {
        challengeDailyLogRepository.delete(challengeDailyLog);
    }

    /*
        챌린지 데일리 로그의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    public void detachLogToUser(List<Long> userIds) {
        challengeDailyLogRepository.detachUser(userIds);
    }

    /*
        관리자가 방장 여부와 관계없이 챌린지를 삭제할 때 사용한다.
        @param challengeId 삭제할 챌린지 id
     */
    @Transactional
    public void deleteChallengeAsAdmin(Long challengeId) {
        deleteChallengeInternal(challengeId);
    }

    /*
        정합성을 유지하기 위해 챌린지 참여자 수를 감소시키는 함수이다.
        만약 챌린지 도메인 내의 참여자 수와 실제 참여자 수가 다른 경우 다시 계산해주는 역할을 한다.
     */
    public void updateChallengeParticipantCount(Challenge challenge) {
        int participantNumber = (int) challengeParticipantRepository.countByChallenge_Id(challenge.getId());  // 전체 참여자 수를 계산한다.

        challenge.synchronizeParticipantNumber(participantNumber);
    }

    /*
        기준에 따라 다음 방장이 될 유저를 선택하는 함수이다.
        현재는 랭킹이 가장 높은 유저를 선택한다.
     */
    public User selectNextOwner(List<ChallengeParticipant> participants) {
        return participants.stream()
                .max(Comparator.comparing(ChallengeParticipant::getRankingScore))
                .map(ChallengeParticipant::getUser)
                .orElse(null);
    }
}
