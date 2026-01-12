// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    유저의 전체 챌린지에 관한 통계를 관리하는 클래스이다.
 */
@Table(name = "user_statistics")
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)  // 유저 당 하나의 통계 값을 가진다.
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // 참여한 전체 챌린지 수이다.
    @Column(name = "total_challenge_count", nullable = false)
    private int totalChallengeCount;

    // 현재 연속으로 챌린지 인증에 성공한 일 횟수이다.
    @Column(name = "current_streak", nullable = false)
    private int currentStreak;

    // 최장 연속으로 챌린지 인증에 성공한 일 횟수이다.
    @Column(name = "max_streak", nullable = false)
    private int maxStreak;

    // 마지막 통계 갱신 시각이다.
    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Builder
    public UserStatistics(User user) {
        this.user = user;
        this.totalChallengeCount = 0;
        this.currentStreak = 0;
        this.maxStreak = 0;
    }

    /*
        챌린지 하나에 새로 참여했을 때 호출되는 함수이다.
     */
    public void increaseChallengeCount() {
        this.totalChallengeCount++;
    }
}
