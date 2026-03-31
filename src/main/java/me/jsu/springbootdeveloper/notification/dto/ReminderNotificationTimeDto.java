// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/*
    실패 방지 리마인더(주간 리마인더)의 전역 시간 설정/조회에 사용하는 DTO이다.
    notificationTime은 "시(hour) 단위"로만 의미가 있으며, 분/초는 서버에서 0으로 고정한다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderNotificationTimeDto {

    @NotNull(message = "리마인더 시간은 필수입니다.")
    private LocalTime notificationTime;  // 리마인더를 받을 시간 (예: 21:00, 시 단위)
}

