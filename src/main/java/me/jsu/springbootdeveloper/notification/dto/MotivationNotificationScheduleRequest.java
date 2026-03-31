// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    동기부여 메시지 알림 설정 요청 DTO이다.
    클라이언트가 "동기부여 알림 활성화 여부"만 서버에 전달할 때 사용한다.
    발송 시각은 서버에서 오전 10시로 고정된다.
 */
@Getter
@NoArgsConstructor
public class MotivationNotificationScheduleRequest {
    @NotNull(message = "활성화 여부는 필수입니다.")
    private Boolean enabled;  // 알림 활성화 여부
}
