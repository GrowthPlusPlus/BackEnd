// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.notification.service.MotivationNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;

/*
    매일 오전 10시에 사용자에게 동기부여 메시지를 발송하는 스케줄러이다.
    더 이상 사용자별 시간 설정은 사용하지 않고, enabled 여부만 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotivationNotificationScheduler {
    private final MotivationNotificationService motivationNotificationService;

    /*
        매일 오전 10:00(Asia/Seoul 기준)에 한 번 실행되어,
        동기부여 알림이 활성화된 모든 사용자에게 메시지를 발송한다.

        cron 표현식: "0 0 10 * * *"
        - 0: 초 (0초)
        - 0: 분 (정각)
        - 10: 시 (10시)
        - *: 일 (매 일)
        - *: 월 (매 월)
        - *: 요일 (매 요일)
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    public void sendMotivationNotifications() {
        // 1. 현재 시간을 로깅용으로만 사용한다.
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // 2. 해당 '시(hour)'에 알림을 받도록 설정된 사용자들에게 동기부여 메시지를 발송한다.
        try {
            motivationNotificationService.sendMotivationNotifications(currentTime);
        } catch (Exception e) {
            // 스케줄러 실행 중 오류가 발생해도 다음 실행에 영향을 주지 않도록 로그만 남긴다.
            log.error("동기부여 메시지 발송 중 오류 발생 (시간: {}): {}",
                    currentTime, e.getMessage(), e);
        }
    }
}
