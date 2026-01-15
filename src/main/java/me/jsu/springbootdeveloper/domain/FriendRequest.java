// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    친구 요청 정보를 관리하기 위한 도메인 클래스이다.
    서비스 레벨에서 스스로에게 친구 요청을 하지 못하도록 제한해야 한다.
    또한 서비스에서 친구 요청을 보내기전에 친구 DB를 조회하여 이미 친구 상태인지 검사해야 한다.
    마지막으로 A -> B 요청이 존재할때 B -> A 요청을 막기 위해 해당 DB의 양방향 검사 함수를 호출하여 중복 요청을 방지해야 한다.
 */
@Table(
        name = "friend_request",
        uniqueConstraints = {
                @UniqueConstraint(  // 한 유저가 동일한 유저에게 여러번 친구 요청을 보낼 수 없다.
                        name = "uk_friend_request_from_to",
                        columnNames = {"from_user_id", "to_user_id"}
                )
        },
        indexes = {
                @Index(  // 받은 친구 요청을 인덱싱 한다.
                        name = "idx_to_user_requested",
                        columnList = "to_user_id"),
                @Index(  // 보낸 친구 요청을 인덱싱 한다.
                        name = "idx_from_user",
                        columnList = "from_user_id"),
                @Index(  // A -> B, B -> A 요청 방지를 위한 인덱싱이다.
                        name = "idx_request",
                        columnList = "from_user_id, to_user_id, status")
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;  // 친구 요청을 보낸 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;  // 친구 요청을 받은 유저

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FriendRequestStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public FriendRequest(User fromUser, User toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = FriendRequestStatus.REQUESTED;  // 요청 생성 당시엔 유효한 상태가 된다.
    }

    /*
        친구 요청을 받은 경우 실행하는 함수이다.
        REQUESTED 상태에서만 ACCEPTED로 전이가 가능하므로 서비스 레벨에서 예외처리를 해야한다.
     */
    void friendRequestAccepted() {
        this.status = FriendRequestStatus.ACCEPTED;
    }

    /*
        친구 요청을 거부한 경우 실행하는 함수이다.
     */
    void friendRequestRejected() {
        this.status = FriendRequestStatus.REJECTED;
    }

    /*
        친구 요청을 취소한 경우 실행하는 함수이다.
     */
    void friendRequestCanceled() {
        this.status = FriendRequestStatus.CANCELED;
    }
}
