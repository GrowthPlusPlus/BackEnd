// 정성우

package me.jsu.springbootdeveloper.admin.service;

import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.domain.UserYearlyActivity;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    기존과 정책이 바뀌거나 논리 오류로 정합성 문제가 생긴 경우, 해당 문제를 해결해주는 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class AdminRepairService {
    private final UserStatisticsService userStatisticsService;
    private final ChallengeQueryService challengeQueryService;

    @Transactional
    public void rebuildTotalActiveDays() {
        // 모든 유저의 통계를 조회한다.
        List<UserStatistics> statisticsList = userStatisticsService.findAllForRepair();
        // 모든 유저의 모든 잔디를 조회한다.
        List<UserYearlyActivity> yearlyActivities = userStatisticsService.findAllUserYearlyActivityForRepair();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int index = today.getDayOfYear() - 1;  // 오늘이 365일 중에 몇 번째 인덱스인지 찾는다.

        // 유저 각각에 대한 총 수행 일수의 맵이다. (유저 id, 수행 일수)
        Map<Long, Integer> totalActiveDaysMap = new HashMap<>();

        // 모든 년도의 잔디를 조회하여 각 유저에 맞는 id에 총 수행 일수를 증가시킨다.
        for (UserYearlyActivity yearlyActivity : yearlyActivities) {
            int successDays = 0;

            // dayBitCount 배열을 순회하며 하루에 하나 이상의 챌린지를 수행한 경우엔 성공 일수를 증가시킨다.
            for (int count : yearlyActivity.getDayBitCount()) {
                if (count > 0) {
                    successDays++;
                }
            }

            // 해당 잔디의 유저 id를 조회한다.
            Long userId = yearlyActivity.getUser().getId();

            // 현재 연도라면 아직 잔디에 반영되지 않은 오늘 인증을 임시 반영한다.
            if (yearlyActivity.getYear() == today.getYear()) {
                int todayCount = challengeQueryService.countByPostDateWithChallenge(userId, today);

                // 오늘 하나라도 수행한 챌린지가 있다면 인증 일수를 증가시킨다.
                if (yearlyActivity.getDayBitCount()[index] == 0 && todayCount > 0) {
                    successDays++;
                }
            }

            // 같은 유저의 여러 연도 활동일수를 누적한다.
            totalActiveDaysMap.merge(userId, successDays, Integer::sum);
        }

        // 각 유저의 통계에 대해 순회한다.
        for (UserStatistics statistics : statisticsList) {
            // 통계의 유저 id를 조회한다.
            Long userId = statistics.getUser().getId();

            // 모든 연도의 잔디를 순회하며 유저별 총 수행 일수를 계산한다.
            statistics.replaceTotalActiveDays(totalActiveDaysMap.getOrDefault(userId, 0));
        }
    }
}
