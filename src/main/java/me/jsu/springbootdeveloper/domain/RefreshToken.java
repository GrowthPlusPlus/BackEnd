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
    DB에 저장하기 위한 리프레시 토큰에 관한 클래스이다.
 */
@Table(
        name = "refresh_token",
        indexes = {
                @Index(  // 유저는 당 하나의 리프레시 토큰을 가지므로 유니크하다. 또한 id 검색을 인덱싱 한다.
                        name = "uk_refresh_token_user",
                        columnList = "user_id",
                        unique = true
                ),
                @Index(  // 토큰을 통한 검색을 인덱싱 한다.
                        name = "idx_refresh_token_value",
                        columnList = "refresh_token"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)  // 유저 당 하나의 리프레시 토큰만 갖는다.
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @CreatedDate  // 리프레시 토큰이 생성된 시간이다.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(User user, String refreshToken) {
        this.user = user;
        this.refreshToken = refreshToken;
    }

    /*
        유저의 리프레시 토큰을 업데이트 할 때 사용하는 함수로, OAuth 로그인 과정에서만 사용한다.
     */
    public void update(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
    }
}
