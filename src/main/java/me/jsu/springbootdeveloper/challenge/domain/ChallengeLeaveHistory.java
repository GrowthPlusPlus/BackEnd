// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    챌린지를 스스로 나가거나 강퇴된 사용자를 남기는 엔티티이다.
    참여 행은 나갈 때 삭제되므로, 같은 챌린지에 다시 들어오지 못하게 이 기록으로 판단한다.
 */
@Table(
        name = "challenge_leave_history",
        uniqueConstraints = {
                @UniqueConstraint(  // 같은 사용자가 같은 챌린지를 두 번 기록하지 않게 한다.
                        name = "uk_leave_user_challenge",
                        columnNames = {"user_id", "challenge_id"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 시각을 자동으로 기록한다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeLeaveHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 나가거나 강퇴된 시각이다.

    /*
        나가기 또는 강퇴 기록을 만든다.

        @param challenge 사용자가 떠난 챌린지
        @param user 나가거나 강퇴된 사용자
     */
    public ChallengeLeaveHistory(Challenge challenge, User user) {
        this.challenge = challenge;
        this.user = user;
    }
}
