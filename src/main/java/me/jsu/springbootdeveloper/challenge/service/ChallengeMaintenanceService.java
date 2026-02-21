// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeWeeklyStatusRepository;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeUtils;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeMaintenanceService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;
    private final UserStatisticsService userStatisticsService;

    private static final int BATCH_SIZE = 500;  // 해당 배치 사이즈로 처리한다.

    // 오늘 챌린지를 몇 명이 수행했는지 알아내기 위해 날짜를 보정한다.
    public void syncToday(LocalDate today) {
        Long lastId = 0L;
        List<Challenge> batch;

        do {
            // 현재 진행중인 챌린지들을 검색한다.
            batch = challengeRepository.findActiveChallengesAfterId(today, lastId, PageRequest.of(0, BATCH_SIZE));

            for (Challenge challenge : batch) {
                challenge.syncToday(today);  // 무조건 오늘로 갱신한다.
                lastId = challenge.getId();  // 커서를 이동한다.
            }
        } while (!batch.isEmpty());
    }

    /*
        하루가 지났을 때, 유저가 어제 챌린지를 수행하지 않았다면 연속 일수를 초기화 하는 함수이다.
     */
    public void resetStreakIfBroken(LocalDate today) {
        // 배치를 처리하기 위해 커서 기반으로 검색한다.
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        do {
            // 진행중인 챌린지들을 배치 사이즈로 검색한다.
            batch = challengeParticipantRepository.findAllInProgressAfterId(lastId, PageRequest.of(0, BATCH_SIZE));
            for (ChallengeParticipant cp : batch) {
                // 연속 수행 이력이 없는 경우이다.
                if (cp.getCurrentStreakEndDate() == null) {
                    cp.initCurrentStreakDays();

                    lastId = cp.getId();
                    continue;  // NPE를 방지한다.
                }

                LocalDate yesterday = today.minusDays(1);  // 어제 날짜
                if (!cp.getCurrentStreakEndDate().isEqual(yesterday)) {  // 마지막 성공 날짜가 어제가 아니면 연속 수행이 끊긴 것이므로 초기화 한다.
                    cp.initCurrentStreakDays();
                }

                lastId = cp.getId();  // 커서를 이동시킨다.
            }
        } while (!batch.isEmpty());  // 조회할 데이터가 없을 때까지 반복한다.
    }

    /*
        챌린지의 주간 상태를 판단하는 함수이다. 해당 주에 실패하면 챌린지 자체를 실패한다.
        한 번에 모든 값을 가져오는 것은 부하가 크므로 batch사이즈로 나누어 계산한다.
     */
    public void failWeeklyStatuses(LocalDate today) {
        // 배치를 처리하기 위해 커서 기반으로 검색한다.
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        do {
            // 진행중인 챌린지들을 배치 사이즈로 검색한다.
            batch = challengeParticipantRepository.findAllInProgressAfterId(lastId, PageRequest.of(0, BATCH_SIZE));
            for (ChallengeParticipant cp : batch) {
                checkWeeklyFailure(cp, today);  // 하나씩 검사한다.
                lastId = cp.getId();  // 커서를 이동시킨다.
            }
        } while (!batch.isEmpty());  // 조회할 데이터가 없을 때까지 반복한다.
    }

    /*
        하루가 지났을 때, 챌린지가 실패했는지 하나씩 검사해주는 함수이다.
     */
    private void checkWeeklyFailure(ChallengeParticipant cp, LocalDate today) {
        Challenge challenge = cp.getChallenge();  // 해당 주간 상태가 속한 챌린지이다.

        // 해당 주간 상태의 유효 범위를 구한다.
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));  // 날짜를 기준으로 해당 주를 구한다.
        LocalDate weekEnd = weekStart.plusDays(6);

        // 챌린지 주간 상태를 유저 id, 챌린지 id, 해당 주로 검색한다. 해당 값은 주에 한 번도 챌린지를 수행하지 않았다면 없을 수 있다.
        ChallengeWeeklyStatus weeklyStatus = challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                        cp.getUser().getId(),
                        cp.getChallenge().getId(),
                        weekStart
        ).orElse(null);

        int successCount;
        if (weeklyStatus != null) {
            successCount =  weeklyStatus.getSuccessCount();  // 해당 주에 성공한 횟수
        } else {  // Optional.empty()인 케이스로 해당 주에 한 번도 수행하지 않았으므로 0 처리한다.
            successCount = 0;
        }

        // 부분 주 보정이 적용된 최소 주간 성공 횟수로, 첫 주차나 마지막 주차를 위해 사용한다.
        int adjustedRequired = ChallengeUtils.calculateAdjustedRequired(challenge, weekStart, weekEnd);

        /*
            effectiveStart와 effectiveEnd는 남은 가능 일수를 계산하기 위해 필요한 값이다.
            effectiveStart는 12시가 넘어간 시점이므로 내일부터 챌린지를 수행 가능한 날짜에 포함한다.
         */
        LocalDate effectiveStart;
        if (today.plusDays(1).isAfter(challenge.getStartDate())) {  // 내일이 챌린지 시작 이후인지 검사한다.
            effectiveStart = today.plusDays(1);  // 보통은 내일부터 수행 가능한 날로 정한다.
        }
        else {
            effectiveStart = challenge.getStartDate();  // 방어코드이다.
        }

        // 챌린지 종료일과 이번주 종료일 중, 더 이른 날짜를 수행 가능한 날짜로 잡는다. 주가 더 남았어도 챌린지가 종료되면 수행이 불가능하기 때문이다.
        LocalDate effectiveEnd;
        if (challenge.getEndDate().isBefore(weekEnd)) {  // 챌린지 종료가 주 마지막 날보다 더 빠른 경우이다.
            effectiveEnd = challenge.getEndDate();
        }
        else {  // 일반적인 케이스이다.
            effectiveEnd = weekEnd;
        }

        long remainingDays = 0;  // 앞으로 성공 횟수를 추가로 올릴 수 있는 최대 횟수의 상한이다.
        if (!effectiveStart.isAfter(effectiveEnd)) {  // 시작일이 마지막 수행일보다 뒤가 아닌 경우이다. 즉 수행 가능한 날짜가 남았다.
            // 날짜 범위는 양 끝을 포함하므로 +1을 해야 한다. ex) start: 8/11, end: 8/11
            remainingDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
        }

        // 조기 실패 판정을 결정한다. 요구된 최소 횟수가 지금까지 수행한 횟수 + 남은 수행 가능 횟수보다 적으면 실패한다.
        if (successCount + remainingDays < adjustedRequired) {
            if (weeklyStatus != null) {  // 해당 주에 한 번이라도 수행한 경우이다.
                weeklyStatus.markFail();  // 주간 상태를 실패로 바꾼다.
            }
            cp.markFail(today);  // 챌린지 상태를 실패로 바꾼다.
            userStatisticsService.updateFailedChallengeCount(cp.getUser().getId());  // 실패한 챌린지 개수를 늘린다.
        }
    }

    /*
        챌린지의 최종 성공을 판정하는 함수이다.
     */
    public void finalizeChallenges(LocalDate today) {
        Long lastId = 0L;
        List<ChallengeParticipant> batch;  // 성능을 위해 배치 사이즈로 나눈다.

        do {
            // 오늘을 기준으로 진행중이면서, 종료일인 챌린지들을 구한다.
            batch = challengeParticipantRepository.findEndedInProgressAfterId(lastId, today, PageRequest.of(0, BATCH_SIZE));

            // 위의 주간 상태 검사를 먼저 통과했으므로, FAIL되지 않은 챌린지들은 모두 성공처리된다.
            for (ChallengeParticipant cp : batch) {
                cp.markSuccess(cp.getChallenge().getEndDate());  // 챌린지 상태를 성공으로 바꾼다.
                userStatisticsService.updateCompletedChallengeCount(cp.getUser().getId());  // 성공한 챌린지 개수를 늘린다.

                lastId = cp.getId();  // 커서를 이동한다
            }
        } while (!batch.isEmpty());
    }
}
