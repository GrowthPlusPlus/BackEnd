// 정성우

package me.jsu.springbootdeveloper.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    유저의 친구 관계를 표현하기 위한 도메인 클래스이다.

    다음과 같은 설정을 서비스 레벨에서 구현한다.
    작은 유저 id가 user1에 들어가도록하여 유니크 옵션과 함께 중복 친구 관계를 방지한다.
    if (user1.getId() < user2.getId()) {
        this.user1 = user1;
        this.user2 = user2;
    } else {
        this.user1 = user2;
        this.user2 = user1;
    }
    또한 서비스 레벨에서 자기 자신과 친구가 되지 못하게 방지하는 코드를 작성한다.
 */
@Table(
        name = "friendship",
        uniqueConstraints = {
                @UniqueConstraint(  // 동일한 친구는 중복되어 존재하면 안된다.
                        name = "uk_friendship_user_pair",
                        columnNames = {"user1_id", "user2_id"}
                )
        },
        indexes = {  // 유저 id로 친구 목록을 검색하기 위한 인덱싱이다.
                @Index(
                        name = "idx_friendship_user1",
                        columnList = "user1_id"),
                @Index(
                        name = "idx_friendship_user2",
                        columnList = "user2_id")
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 한 친구관계 엔트리 값에 2개의 유저 엔트리가 연결되므로 many to one 관계이다.
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Friendship(User user1, User user2) {
        this.user1 = user1;
        this.user2 = user2;
    }
}
