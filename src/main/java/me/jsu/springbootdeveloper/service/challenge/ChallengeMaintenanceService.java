// 정성우

package me.jsu.springbootdeveloper.service.challenge;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeWeeklyStatusRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeMaintenanceService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;

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
        챌린지의 주강 상태를 판단하는 함수이다. 해당 주에 실패하면 챌린지 자체를 실패한다.
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

    private void checkWeeklyFailure(ChallengeParticipant cp, LocalDate today) {
        Challenge challenge = cp.getChallenge();  // 해당 주간 상태가 속한 챌린지이다.

        // 해당 주간 상태의 유효 범위를 구한다.
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));  // 날짜를 기준으로 해당 주를 구한다.
        LocalDate weekEnd = weekStart.plusDays(6);

        // 챌린지 주간 상태를 유저 id, 챌린지 id, 해당 주로 검색한다. 해당 값은 주에 한 번도 챌린지를 수행하지 않았다면 없을 수 있다.
        Optional<ChallengeWeeklyStatus> weeklyStatusOpt = challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                        cp.getUser().getId(),
                        cp.getChallenge().getId(),
                        weekStart
        );

        int successCount;
        if (weeklyStatusOpt.isPresent()) {
            successCount =  weeklyStatusOpt.get().getSuccessCount();  // 해당 주에 성공한 횟수
        } else {  // Optional.empty()인 케이스로 해당 주에 한 번도 수행하지 않았으므로 0 처리한다.
            successCount = 0;
        }

        // 부분 주 보정이 적용된 최소 주간 성공 횟수로, 첫 주차나 마지막 주차를 위해 사용한다.
        int adjustedRequired = calculateAdjustedRequired(challenge, weekStart, weekEnd);

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
            if (weeklyStatusOpt.isPresent()) {  // 해당 주에 한 번이라도 수행한 경우이다.
                weeklyStatusOpt.get().markFail();  // 주간 상태를 실패로 바꾼다.
            }
            cp.markFail(today);  // 챌린지 상태를 실패로 바꾼다.
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
                cp.markSuccess(cp.getChallenge().getEndDate());
                lastId = cp.getId();  // 커서를 이동한다
            }
        } while (!batch.isEmpty());
    }

    /*
       부분 주 보정 로직이다.
       주는 항상 월요일을 기준으로 시작하므로, 해당 유저가 주에 늦게 시작하여 불이익을 보거나 챌린지 마지막 주에 불이익을 보지 않도록 한다.
       주간 최소 인증 횟수를 비례 조정한다.
     */
    public int calculateAdjustedRequired(Challenge challenge, LocalDate weekStart, LocalDate weekEnd) {
        // 챌린지 시작일과 주 시작일 중 더 늦은 날짜를 사용한다. 따라서 챌린지가 없던 날은 계산에서 제외한다.
        LocalDate effectiveStart;
        if (challenge.getStartDate().isAfter(weekStart)) {  // 챌린지 시작일이 주 시작일보다 더 늦은 경우이다.
            effectiveStart = challenge.getStartDate();  // 주의 시작인 월요일부터가 아닌, 챌린지 시작일부터 계산한다.
        }
        else {  // 일반적인 케이스이다.
            effectiveStart = weekStart;
        }

        // 챌린지 종료일과 주의 종료일 중 더 이른 날짜를 사용한다. 주는 일요일까지지만 챌린지가 금요일에 끝나는 경우 등을 고려한다.
        LocalDate effectiveEnd;
        if (challenge.getEndDate().isBefore(weekEnd)) {  // 챌린지 종료일이 주 종료일보다 빠른 경우이다.
            effectiveEnd = challenge.getEndDate();  // 주의 마지막인 일요일이 아닌, 챌린지 종료일로 계산한다.
        }
        else {  // 일반적인 경우이다.
            effectiveEnd = weekEnd;
        }

        // 이번 주에 챌린지가 실제로 존재한 날짜 수를 계산한다. 날짜의 양 끝을 포함하기 위해 +1을 한다.
        long daysInWeek = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;

        // 원래 주간 목표 * (실제 수행가능한 일수 / 7일) 값을 반올림 하여 최소 인증 횟수를 조정한다.
        return (int) Math.ceil(challenge.getRequiredWeeklyCount() * (daysInWeek / 7.0));
    }
}
