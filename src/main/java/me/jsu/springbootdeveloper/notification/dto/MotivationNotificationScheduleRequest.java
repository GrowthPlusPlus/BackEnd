// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/*
    동기부여 메시지 알림 시간 설정 요청 DTO이다.
    클라이언트가 알림 시간과 활성화 여부를 서버에 전달할 때 사용한다.
 */
@Getter
@NoArgsConstructor
public class MotivationNotificationScheduleRequest {
    @NotNull(message = "알림 시간은 필수입니다.")
    private LocalTime notificationTime;  // 알림을 받을 시간 (예: 09:00)

    @NotNull(message = "활성화 여부는 필수입니다.")
    private Boolean enabled;  // 알림 활성화 여부
}
