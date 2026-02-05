// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/*
    주마다 유저 통계의 대표 실패 챌린지, 챌린지 수행 variance를 계산하는 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class UserWeeklyStatisticsService {
    private final ChallengeService challengeService;
    private final UserStatisticsRepository userStatisticsRepository;

    private static final int BATCH_SIZE = 500;  // 배치 크기
    private static final int AT_LEAST_WEEK = 3;  // 최소 챌린지를 진행한 주차 수
    private static final int WEEKS_TO_LOOK_BACK = 4;  // 편향을 되돌아볼 주차 수이다.
    private static final int MAX_WEEK_FAILED_CHALLENGE = 3;

    /*
       주 종료 시점에 호출하여 주간 정산을 실행한다.
       weekStartDate: 그 주의 시작일(월요일 등)
       weekEndDate: 그 주의 마지막일(일요일 등)
       requiredCount: 주간 최소 인증 횟수
     */
    @Transactional
    public void settleWeekBatch(LocalDate today) {
        Long lastId = 0L;  // 0번 유저부터 모든 통계를 조사한다.
        List<UserStatistics> batch;

        do {
            // 유저 통계를 검색한다.
            batch = userStatisticsRepository.findAllAfterId(lastId, PageRequest.of(0, BATCH_SIZE));

            for (UserStatistics userStats : batch) {
                Long userId = userStats.getUser().getId();

                updateMostFailedChallenge(userStats, userId, today);  // 실패 챌린지를 선정한다.
                updateMostUnstableChallenge(userStats, userId,  WEEKS_TO_LOOK_BACK);  // 분산을 계산한다.

                lastId = userStats.getUser().getId();
            }
        } while (!batch.isEmpty());
    }

    /*
       대표 실패 챌린지를 갱신한다. 실패율이 가장 높은 챌린지가 선정된다.
       최소 n주 이상 성공 경험이 있는 챌린지만 실패 통계 대상으로 삼는다. 조금도 시도하지 않은 챌린지는 제외하기 위함이다.
       실패 챌린지는 4주마다 초기화 된다.
       실패율 = 실패한 주차 개수 / 총 주차 개수
     */
    private void updateMostFailedChallenge(UserStatistics userStats, Long userId, LocalDate today) {
        // 유저가 참여한 모든 실패한 챌린지를 검색한다.
        List<ChallengeParticipant> participants = challengeService.findStatusWithChallengeByUserIdAndStatus(userId, Status.FAIL);

        // 대표 실패 챌린지가 오랜 시간이 지났다면 초기화 한다.
        if (userStats.getMostFailedUpdatedAt() == null || ChronoUnit.WEEKS.between(userStats.getMostFailedUpdatedAt(), today) >= MAX_WEEK_FAILED_CHALLENGE) {
            userStats.updateMostFailed(null, 0.0, today);
        }

        for (ChallengeParticipant p : participants) {
            double rate = p.getFailureRateByWeek();

            if (userStats.getMostFailedChallengeId() != null) {  // 기존에 대표 실패 챌린지가 존재하는 경우이다.
                if (AT_LEAST_WEEK <= p.getSuccessWeeks() &&  // 최소 주를 달성하지 못하고 실패하면 업데이트 하지 않는다.
                        userStats.getMostFailedChallengeFailureRate() < rate) {  // 기존 대표 실패 챌린지보다 실패율이 높으면 업데이트 한다.
                    userStats.updateMostFailed(p.getChallenge().getId(), rate, today);
                }
            }
            else {  // 기존에 대표 챌린지가 없는 경우이다.
                userStats.updateMostFailed(p.getChallenge().getId(), rate, today);
            }
        }
    }

    /*
       불안정 대표(mostUnstable)를 갱신한다.
       최근 N주 weekly successCount의 분산(variance)이 최대인 챌린지 선택
       이 값은 주간 정산에서만 갱신한다.
     */
    private void updateMostUnstableChallenge(UserStatistics userStats, Long userId, int weeksToLookBack) {
        // 참가한 진행중인 챌린지들을 가져와서 각 챌린지마다 최근 N주의 주마다의 성공 횟수를 가진 배열을 만든 뒤 분산을 계산한다.
        List<ChallengeParticipant> participants = challengeService.findInProgressWithChallengeByUserId(userId);

        Long mostUnstableId = null;
        double maxVariance = -1.0;

        for (ChallengeParticipant p : participants) {
            Long challengeId = p.getChallenge().getId();

            // 최근 주의 인증글 성공 횟수 리스트를 검색한다.
            List<Integer> weeklyCounts = loadRecentWeeklySuccessCounts(userId, challengeId, weeksToLookBack);

            // 해당 챌린지의 최근 주에 수행한 인증글이 아에 없다면 분산 계산에 포함하지 않는다.
            if (weeklyCounts.isEmpty()) {
                continue;
            }

            double variance = varianceOf(weeklyCounts);  // 분산을 구한다.
            if (variance > maxVariance) {  // 최근 챌린지 중, 분산이 가장 큰 챌린지를 찾는다.
                maxVariance = variance;
                mostUnstableId = challengeId;
            }
        }

        if (mostUnstableId != null) {  // 최근 분산 편향이 가장 큰 챌린지를 업데이트 한다.
            userStats.updateMostUnstable(mostUnstableId, maxVariance);
        }
    }

    /*
       최근 n주의 successCount 리스트를 반환한다.
     */
    private List<Integer> loadRecentWeeklySuccessCounts(Long userId, Long challengeId, int weeksToLookBack) {
        return challengeService.findRecentSuccessCounts(userId, challengeId, PageRequest.of(0, weeksToLookBack));
    }

    /*
       분산을 계산한다. (population variance: / n)
     */
    private double varianceOf(List<Integer> xs) {
        double mean = xs.stream().mapToInt(i -> i).average().orElse(0.0);
        return xs.stream()
                .mapToDouble(x -> (x - mean) * (x - mean))
                .average()
                .orElse(0.0);
    }
}