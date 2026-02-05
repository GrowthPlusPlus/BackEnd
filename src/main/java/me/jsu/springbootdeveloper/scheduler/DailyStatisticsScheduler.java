// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.service.UserDailyStatisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/*
    매일 자정마다 유저가 연속으로 챌린지를 수행했는지 검사하는 스케줄러이다.
 */
@Component
@RequiredArgsConstructor
public class DailyStatisticsScheduler {
    private final UserDailyStatisticsService userDailyStatisticsService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailySettlement() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        userDailyStatisticsService.settleDailyStatistics(today.minusDays(1));  // 어제 하루를 평가한다.
    }
}
