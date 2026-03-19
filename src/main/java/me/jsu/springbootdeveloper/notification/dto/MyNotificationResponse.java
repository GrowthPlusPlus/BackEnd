// 정성우

package me.jsu.springbootdeveloper.notification.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.notification.domain.Notification;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;

import java.time.LocalDate;
import java.util.List;

/*
    알람 정보와 더 읽어올 알람이 있는지 알려주는 불린 값을 포함하여 실제로 클라이언트에게 전달하는 dto이다.
 */
@Getter
@Builder
public class MyNotificationResponse {
    private String profileImageUrl; //ImageUrl-> profileImageUrl로 수정
    private Long targetId;
    private String message;  // 알림 내용이다.
    private NotificationType type;  // 알림 유형이다.
    private boolean isRead;  // 읽음 여부를 나타낸다.
    private LocalDate created;  // 메세지가 생성된 시간으로 UTC 표준시이다.
}
