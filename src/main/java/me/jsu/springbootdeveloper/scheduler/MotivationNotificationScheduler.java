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
    매일 지정된 시간에 사용자에게 동기부여 메시지를 발송하는 스케줄러이다.
    매시 정각마다 실행되어 해당 '시(hour)'에 알림을 받도록 설정된 사용자들에게 메시지를 발송한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MotivationNotificationScheduler {
    private final MotivationNotificationService motivationNotificationService;

    /*
        매시 정각(예: 09:00, 10:00, 11:00)에 실행되어,
        사용자가 설정한 알림 시간의 '시(hour)'에 해당하는 사용자들에게 발송한다.
        
        cron 표현식: "0 0 * * * *"
        - 0: 초 (0초)
        - 0: 분 (정각)
        - *: 시 (매 시)
        - *: 일 (매 일)
        - *: 월 (매 월)
        - *: 요일 (매 요일)
        
        Asia/Seoul 시간대를 사용하여 한국 시간 기준으로 실행된다.
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendMotivationNotifications() {
        // 1. 현재 시간의 '시(hour)'만 사용하므로 분을 0으로 고정한다.
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
