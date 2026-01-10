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
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @CreatedDate  // 리프레시 토큰이 생성된 시간이다.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshToken(Long userId, String refreshToken, LocalDateTime createdAt) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.createdAt = createdAt;  // 리프레시 토큰 생성 시각이다.
    }

    /*
        유저의 리프레시 토큰을 업데이트 할 때 사용하는 함수로, OAuth 로그인 과정에서만 사용한다.
     */
    public RefreshToken update(String newRefreshToken) {
        this.refreshToken = newRefreshToken;
        return this;
    }
}
