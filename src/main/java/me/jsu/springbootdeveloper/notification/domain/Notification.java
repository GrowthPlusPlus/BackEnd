// 정성우

package me.jsu.springbootdeveloper.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Table(
        name = "notification",
        indexes = {
                @Index(
                        name = "idx_user_notification",
                        columnList = "user_id"  // 알림을 유저 id로 검색하기 위한 인덱싱이다.
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 알람 수신자이다.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;  // 알림 타입을 지정한다.

    @Column(nullable = false, length = 1000)
    private String message;  // 알림 내용이다.

    @Column(name = "is_read", nullable = false)
    private boolean isRead;  // 읽음 여부를 나타낸다.

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    //알림페이지에 관련 인물 프로필 조회를 위한 칼럼
    @Column(name = "image_url")
    private String imageUrl;

    //타겟 id
    @Column(name = "target_id")
    private Long targetId;

    @Builder
    public Notification(User user, NotificationType type, String message) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.isRead = false;  // 초기 상태는 읽지 않음이다.
    }

    /*
        메세지를 읽은 경우 해당 함수를 호출하여 메세지를 읽음 상태로 변경한다.
     */
    public void markAsRead() {
        this.isRead = true;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
