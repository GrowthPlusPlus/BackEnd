// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.service.UserWeeklyStatisticsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/*
    주마다 유저의 통계를 업데이트 하기 위한 스케줄러이다.
    일요일 자정이 지나자마자 실행된다.
 */
@Component
@RequiredArgsConstructor
public class WeeklyStatisticsScheduler {
    private final UserWeeklyStatisticsService userWeeklyStatisticsService;

    @Scheduled(cron = "0 0 0 ? * MON", zone = "Asia/Seoul")
    public void runWeeklySettlement() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        userWeeklyStatisticsService.settleWeekBatch(today);
    }
}