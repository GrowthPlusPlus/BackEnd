// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    챌린지에 친구를 초대하기 위한 링크를 저장하는 도메인 클래스이다.
    초대 링크마다 다른 값은 inviteCode이므로 해당 값만 저장한다.
    한 유저가 동일한 챌린지에 대해 여러 초대 링크를 생성할 수 있다.
 */
@Table(
        name = "challenge_invite_link",
        uniqueConstraints = {
                @UniqueConstraint(  // 초대 코드는 반드시 하나만 존재한다.
                        name = "uk_invite_code",
                        columnNames = {"invite_code"}
                )
        },
        indexes = {
                @Index(  // 특정 유저가 생성한 모든 초대 링크를 조회하기 위한 인덱싱이다.
                        name = "idx_user_id_link",
                        columnList = "user_id"
                ),
                @Index(  // 특정 유저가 생성한 유효한 모든 초대 링크를 조회하기 위한 인덱싱이다.
                        name = "idx_user_active",
                        columnList = "user_id, active"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeInviteLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 같은 챌린지에 여러 초대 링크를 생성할 수 있다.
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)  // 같은 사람이 여러 초대 링크를 생성할 수 있다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "invite_code", nullable = false, unique = true, length = 36)
    private String inviteCode; // UUID v4로 생성된 초대 코드로, 우리가 줄 링크는 "초대링크 + inviteCode"로 구성된다.

    @Column(name = "active", nullable = false)
    private boolean active;  // 현재 해당 링크가 사용 가능한지 알려준다.

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChallengeInviteLink(Challenge challenge, User user, String inviteCode, LocalDateTime expiresAt) {
        this.challenge = challenge;
        this.user = user;
        this.inviteCode = inviteCode;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    /*
        링크의 사용 조건의 종료를 서비스에서 판단하여 이 함수를 호출하면, 링크가 비활성화 된다.
        종료 조건으로는 일정 시간의 경과나 동일한 챌린지에 대한 초대 링크 재발급 등이 있을 수 있다.
     */
    public void deactivate() {
        this.active = false;
    }
}
