// 정성우

package me.jsu.springbootdeveloper.service.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.challenge.Status;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.MyChallengeInProgressResponse;
import me.jsu.springbootdeveloper.dto.MyChallengeSuccessOrFailResponse;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/*
    유저가 수행한 챌린지 목록을 상태에 따라 조회하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 해당 서비스는 조회 전용이다.
public class MyChallengeListService {
    private final ChallengeService challengeService;
    private final UserService userService;

    /*
        현재 진행중인 챌린지를 검색하는 함수이다.
     */
    public List<MyChallengeInProgressResponse> getMyChallenges(String email, Status status) {
        User user = userService.findByEmail(email);

        // 참여한 챌린지들을 검색한다.
        List<ChallengeParticipant> challengeParticipants = challengeService.findStatusWithChallengeByUserIdAndStatus(user.getId(), status);

        // 인증률 계산을 위한 알고리즘 수행

        // 얻은 값을 매핑하여 반환한다.
        return challengeParticipants.stream()
                .map(cp -> {
                    LocalDate startDate = cp.getChallenge().getStartDate();
                    LocalDate endDate = cp.getChallenge().getEndDate();
                    LocalDate today = LocalDate.now();

                    LocalDate effectiveEnd = today;
                    if (today.isAfter(endDate)) {  // 이미 끝난 챌린지인데 오류로 검색된 경우
                        effectiveEnd = endDate;
                    }

                    long elapsedDays = 0;  // 경과일
                    if (effectiveEnd.isAfter(startDate)) {  // 챌린지가 시작일 이후인 경우에만 계산
                        elapsedDays = ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1;  // 시작한 당일도 경과일로 계산한다.
                    }

                    return MyChallengeInProgressResponse.builder()
                            .challengeId(cp.getChallenge().getId())
                            .title(cp.getChallenge().getTitle())
                            .requiredWeeklyCount(cp.getChallenge().getRequiredWeeklyCount())
                            .todaySuccessCount(cp.getChallenge().getTodaySuccessCount())
                            .participantNumber(cp.getChallenge().getParticipantNumber())
                            .duringDate(elapsedDays)
                            .endDate(cp.getChallenge().getEndDate())  // 완료일
                            .achievementRate(0) // 계산 정책 정의 필요
                            .status(status)
                            .build();
                        }
                )
                .toList();
    }

    /*
        완료했거나, 실패한 챌린지를 검색하는 함수이다.
     */
    public List<MyChallengeSuccessOrFailResponse> getMyChallengesSuccessOrFail(String email, Status status) {
        User user = userService.findByEmail(email);

        // 참여한 챌린지들을 검색한다.
        List<ChallengeParticipant> challengeParticipants = challengeService.findStatusWithChallengeByUserIdAndStatus(user.getId(), status);

        // 달성율 계산을 위한 알고리즘 수행

        // 얻은 값을 매핑하여 반환한다.
        return challengeParticipants.stream()
                .map(cp -> {
                    LocalDate endDate;
                    if (status == Status.SUCCESS) {
                        endDate = Objects.requireNonNull(cp.getCompletedAt());  // 완료일이 null이면 오류가 발생한다.
                    }
                    else {
                        endDate = Objects.requireNonNull(cp.getFailedAt());  // 실패일이 null이면 오류가 발생한다.
                    }

                    return MyChallengeSuccessOrFailResponse.builder()
                            .challengeId(cp.getChallenge().getId())
                            .title(cp.getChallenge().getTitle())
                            .duringDate(ChronoUnit.DAYS.between(cp.getChallenge().getStartDate(), endDate))
                            .endDate(endDate)  // 완료 또는 실패일
                            .achievementRate(0) // 계산 정책 정의 필요
                            .status(status)
                            .build();
                })
                .toList();
    }
}
