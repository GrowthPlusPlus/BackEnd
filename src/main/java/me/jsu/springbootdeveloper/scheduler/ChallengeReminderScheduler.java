// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.notification.service.ChallengeReminderNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/*
    사용자가 참여 중인 챌린지에 대해,
    해당 주의 인증 횟수를 아직 모두 채우지 못했고
    주간 마감 시간(일요일 24시) 60분 전(일요일 23시)까지 인증을 완료하지 못한 경우
    리마인더 푸시 알림을 발송하는 스케줄러이다.

    - 매주 일요일 23:00(Asia/Seoul 기준)에 실행된다.
    - 실행 시점에 이번 주 기준으로 아직 조건을 만족하지 못한 사용자만을 대상으로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeReminderScheduler {

    private final ChallengeReminderNotificationService challengeReminderNotificationService;

    /*
        매주 일요일 23:00(Asia/Seoul 기준)에 실행되어,
        이번 주 챌린지 인증을 아직 완료하지 못한 유저들에게
        리마인더 알림을 발송한다.
     */
    @Scheduled(cron = "0 0 23 ? * SUN", zone = "Asia/Seoul")
    public void sendWeeklyChallengeReminders() {
        // 1. 한국 시간(Asia/Seoul) 기준 오늘 날짜를 계산한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 2. 서비스 레이어에 위임하여, 실제 배치 로직을 수행한다.
        try {
            challengeReminderNotificationService.sendWeeklyChallengeReminders(today);
        } catch (Exception e) {
            // 스케줄러 실행 중 오류가 발생해도 다음 실행에 영향을 주지 않도록 로그만 남긴다.
            log.error("주간 챌린지 리마인더 스케줄러 실행 중 오류 발생 (today={}): {}",
                    today, e.getMessage(), e);
        }
    }
}

