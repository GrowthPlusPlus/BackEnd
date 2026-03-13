// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;

import java.time.LocalTime;

/*
    동기부여 메시지 알림 시간 설정 응답 DTO이다.
    서버가 사용자의 알림 시간 설정 정보를 클라이언트에 전달할 때 사용한다.
 */
@Getter
@Builder
public class MotivationNotificationScheduleResponse {
    private LocalTime notificationTime;  // 알림을 받을 시간
    private boolean enabled;  // 알림 활성화 여부

    /*
        UserNotificationSchedule 엔티티를 DTO로 변환한다.
        
        @param schedule 알림 시간 설정 엔티티
        @return DTO 객체 (schedule이 null이면 null 반환)
     */
    public static MotivationNotificationScheduleResponse from(UserNotificationSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        return MotivationNotificationScheduleResponse.builder()
                .notificationTime(schedule.getNotificationTime())
                .enabled(schedule.isEnabled())
                .build();
    }
}
