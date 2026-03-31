// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;

/*
    동기부여 메시지 알림 설정 응답 DTO이다.
    서버가 사용자의 동기부여 알림 활성화 여부를 클라이언트에 전달할 때 사용한다.
    발송 시각은 항상 오전 10시로 고정되므로, 시간 정보는 별도로 내리지 않는다.
 */
@Getter
@Builder
public class MotivationNotificationScheduleResponse {
    private boolean enabled;  // 알림 활성화 여부

    /*
        UserNotificationSchedule 엔티티를 DTO로 변환한다.
        
        @param schedule 알림 설정 엔티티
        @return DTO 객체 (schedule이 null이면 기본값 false로 응답)
     */
    public static MotivationNotificationScheduleResponse from(UserNotificationSchedule schedule) {
        if (schedule == null) {
            return MotivationNotificationScheduleResponse.builder()
                    .enabled(false)
                    .build();
        }
        return MotivationNotificationScheduleResponse.builder()
                .enabled(schedule.isEnabled())
                .build();
    }
}
