// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeRankingService;
import me.jsu.springbootdeveloper.scheduler.schema.RequireRankingScoreSchema;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
    매일 11시 50분마다 전체 랭킹을 집계하는 스케줄러이다.
 */
@Component
@RequiredArgsConstructor
public class ChallengeRankingScheduler {
    private final ChallengeRankingService challengeRankingService;

    //@Scheduled(cron = "*/20 * * * * *", zone = "Asia/Seoul")
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    @RequireRankingScoreSchema  // 스키마 준비가 안 됐으면 바로 종료한다.
    public void runDailyRankingUpdate() {
        challengeRankingService.updateRankingForChallenge();
    }
}
