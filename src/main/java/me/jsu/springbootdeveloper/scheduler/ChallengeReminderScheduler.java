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
    유저 전역 리마인더 시간(실패 방지 리마인더 시간)과 챌린지별 리마인더 토글이 모두 활성화되어 있는 경우
    "실패 방지 리마인더" 푸시 알림을 발송하는 스케줄러이다.

    - 매주 일요일 00시부터 23시까지, 매 정각(1시간 간격)마다 실행된다.
    - 각 실행 시점의 시(hour)에 대해, 그 시간으로 리마인더를 설정한 유저들만 대상으로 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeReminderScheduler {

    private final ChallengeReminderNotificationService challengeReminderNotificationService;

    /*
        매주 일요일 00:00 ~ 23:00(Asia/Seoul 기준)에 매시 정각마다 실행되어,
        이번 주 챌린지 인증을 아직 완료하지 못한 유저들 중
        - 유저 전역 리마인더 시간을 현재 시(hour)로 설정했고
        - 챌린지별 리마인더 토글이 켜져 있는 경우에만
        실패 방지 리마인더 알림을 발송한다.

        cron 표현식: "0 0 0-23 ? * SUN"
        - 0: 초 (0초)
        - 0: 분 (정각)
        - 0-23: 시 (일요일 0시 ~ 23시)
        - ?: 일(day-of-month)은 사용하지 않음
        - SUN: 요일 (일요일)
     */
    @Scheduled(cron = "0 0 0-23 ? * SUN", zone = "Asia/Seoul")
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

