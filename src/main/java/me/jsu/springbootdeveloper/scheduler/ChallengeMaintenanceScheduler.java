// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.service.challenge.ChallengeMaintenanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/*
    해당 스케줄러 클래스는 매일 0시 (Asia/Seoul 기준)마다 다음과 같은 일을 수행한다.
    1. 현재 진행중인 챌린지의 성공/실패 확정
    2. 현재 진행준인 챌린지의 날짜 변경 보정 (오늘 챌린지를 수행한 참여자 수를 계산하기 위해 필요하다.)
 */
@Component
@RequiredArgsConstructor
public class ChallengeMaintenanceScheduler {
    private final ChallengeMaintenanceService challengeMaintenanceService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void dailyJob() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        challengeMaintenanceService.syncToday(today);  // 진행중인 챌린지 날짜를 동기화 한다.
        challengeMaintenanceService.failWeeklyStatuses(today);  // 이번주 챌린지 성공/실패 여부를 판단한다.
        challengeMaintenanceService.finalizeChallenges(today);  // 챌린지 종료를 판단한다.
    }
}
