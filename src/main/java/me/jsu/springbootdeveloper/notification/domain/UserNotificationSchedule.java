// 정성우

package me.jsu.springbootdeveloper.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;

/*
    사용자별 동기부여 메시지 알림 시간 설정을 저장하는 엔티티이다.
    각 사용자는 자신이 원하는 시간에 동기부여 메시지를 받을 수 있다.
 */
@Table(
        name = "user_notification_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_notification_schedule",
                        columnNames = {"user_id"}  // 한 사용자당 하나의 알림 시간 설정만 가질 수 있다.
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_notification_schedule_enabled",
                        columnList = "enabled, notification_time"  // 활성화된 사용자들을 시간별로 조회하기 위한 인덱싱이다.
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class UserNotificationSchedule {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;  // 알림을 받을 사용자이다.

    @Column(name = "notification_time", nullable = false)
    private LocalTime notificationTime;  // 매일 알림을 받을 시간이다. (예: 09:00)

    @Column(name = "enabled", nullable = false)
    private boolean enabled;  // 동기부여 알림 활성화 여부이다. 기본값은 true이다.

    @Column(name = "daily_reminder_push_notification_enabled", nullable = false)
    private boolean dailyReminderPushNotificationEnabled = true;  // 전체 일일(주간) 리마인더 푸시 알림 활성화 여부이다.

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 설정이 생성된 시간이다.

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;  // 설정이 마지막으로 수정된 시간이다.

    @Builder
    public UserNotificationSchedule(
            User user,
            LocalTime notificationTime,
            boolean enabled,
            boolean dailyReminderPushNotificationEnabled) {

        this.user = user;
        this.notificationTime = notificationTime;
        this.enabled = enabled;
        this.dailyReminderPushNotificationEnabled = dailyReminderPushNotificationEnabled;
    }

    /*
        알림 시간을 변경하는 함수이다.
        @param notificationTime 새로운 알림 시간
     */
    public void updateNotificationTime(LocalTime notificationTime) {
        this.notificationTime = notificationTime;
    }

    /*
        동기부여 알림 활성화 여부를 변경하는 함수이다.
        @param enabled 활성화 여부
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*
        전체 일일(주간) 리마인더 푸시 알림 활성화 여부를 변경하는 함수이다.
        @param enabled 활성화 여부
     */
    public void setDailyReminderPushNotificationEnabled(boolean enabled) {
        this.dailyReminderPushNotificationEnabled = enabled;
    }
}
