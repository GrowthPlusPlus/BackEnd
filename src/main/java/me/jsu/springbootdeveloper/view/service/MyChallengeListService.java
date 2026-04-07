// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeUtils;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.view.dto.MyChallengeInProgressResponse;
import me.jsu.springbootdeveloper.view.dto.MyChallengeSuccessOrFailResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/*
    유저가 수행한 챌린지 목록을 상태에 따라 조회하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 해당 서비스는 조회 전용이다.
public class MyChallengeListService {
    private final ChallengeQueryService challengeQueryService;
    private final UserService userService;

    /*
        현재 진행중인 챌린지를 검색하는 함수이다.
     */
    public List<MyChallengeInProgressResponse> getMyChallenges(String email, Status status, boolean onlyTwo) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 참여한 챌린지들을 검색한다.
        List<ChallengeParticipant> challengeParticipants = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(user.getId(), status);

        // 클라이언트가 챌린지 목록을 2개만 요청한 경우, 챌린지 개수를 2개로 줄인다.
        if (onlyTwo) {
            challengeParticipants = challengeParticipants.stream()
                    .limit(2)
                    .toList();
        }

        /*
            유저가 요청한 날짜의 하루 데일리 로그를 검색한다. 하나도 수행하지 않았다면 빈 리스트를 반환한다.
         */
        Map<Long, ChallengeDailyLog> dailyLogMap;
        List<ChallengeDailyLog> challengeDailyLogs = challengeQueryService.findByUser_IdAndPostDate(user.getId(), today);
        // 데일리 로그 list를 map으로 바꾸어서 챌린지 id와 데일리 로그의 챌린지 id를 바로 매핑한다.
        dailyLogMap = challengeDailyLogs.stream().collect(Collectors.toMap(
                log -> log.getChallenge().getId(),  // 챌린지 id를 map key로 설정한다.
                log -> log,
                (existing, replacement) -> existing  // challengeId 키값 중복 시, 기존 값으로 사용한다.
        ));

        // 얻은 값을 매핑하여 반환한다.
        return challengeParticipants.stream()
                .map(cp -> {
                    LocalDate startDate = cp.getJoinDate();  // 유저가 챌린지를 시작한 날을 기준으로 한다.
                    LocalDate endDate = cp.getChallenge().getEndDate();

                    LocalDate effectiveEnd = today;
                    if (today.isAfter(endDate)) {  // 이미 끝난 챌린지인데 오류로 검색된 경우
                        effectiveEnd = endDate;
                    }

                    long elapsedDays = 0;  // 경과일
                    if (effectiveEnd.isAfter(startDate)) {  // 챌린지가 시작일 이후인 경우에만 계산
                        elapsedDays = ChronoUnit.DAYS.between(startDate, effectiveEnd) + 1;  // 시작한 당일도 경과일로 계산한다.
                    }

                    // 남은 챌린지 수행 일수를 계산한다.
                    long dDay = ChallengeUtils.calculateDDay(today, endDate);

                    // 오늘 데일리 로그 여부를 판별한다.
                    ChallengeDailyLog dailyLog = dailyLogMap.get(cp.getChallenge().getId());

                    // 오늘 인증을 했다면 로그가 있으므로 true, 인증을 하지 않았다면 로그가 존재하지 않으므로 false이다.
                    boolean isDoIt = (dailyLog != null);

                    return MyChallengeInProgressResponse.builder()
                            .challengeId(cp.getChallenge().getId())
                            .title(cp.getChallenge().getTitle())
                            .requiredWeeklyCount(cp.getChallenge().getRequiredWeeklyCount())
                            .todaySuccessCount(cp.getChallenge().getTodaySuccessCount())
                            .participantNumber(cp.getChallenge().getParticipantNumber())
                            .duringDate(elapsedDays)
                            .endDate(cp.getChallenge().getEndDate())  // 완료일
                            .achievementRate(calculateAchievementRatePercent(cp))  // 달성률을 계산하여 반환한다.
                            .status(status)
                            .dueToDate(dDay)
                            .currentStreak(cp.getCurrentStreakDays())  // 연속 수행 일수이다.
                            .doIt(isDoIt)
                            .build();
                        }
                )
                .toList();
    }

    /*
        완료했거나, 실패한 챌린지를 검색하는 함수이다.
     */
    public List<MyChallengeSuccessOrFailResponse> getMyChallengesSuccessOrFail(String email, Status status, boolean onlyTwo) {
        User user = userService.findByEmail(email);

        // 참여한 챌린지들을 검색한다.
        List<ChallengeParticipant> challengeParticipants = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(user.getId(), status);

        // 클라이언트가 챌린지 목록을 2개만 요청한 경우, 챌린지 개수를 2개로 줄인다.
        if (onlyTwo) {
            challengeParticipants = challengeParticipants.stream()
                    .limit(2)
                    .toList();
        }

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
                            .achievementRate(calculateAchievementRatePercent(cp))  // 달성률을 계산하여 반환한다.
                            .status(status)
                            .build();
                })
                .toList();
    }

    /*
        챌린지의 달성률을 계산해주는 함수이다.
     */
    public int calculateAchievementRatePercent(ChallengeParticipant participant) {
        int totalWeeks = participant.getTotalWeeks();

        if (totalWeeks == 0) {
            return 0;
        }

        // 현재 진행한 주차 수와 총 주차 수로 달성률을 계산한다.
        return (int) Math.round((double) participant.getSuccessWeeks() * 100 / totalWeeks);
    }
}
