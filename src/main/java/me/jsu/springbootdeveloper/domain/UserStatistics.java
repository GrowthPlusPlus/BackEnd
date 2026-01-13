// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/*
    유저의 전체 챌린지에 관한 통계를 관리하는 클래스이다.
 */
@Table(
        name = "user_statistics",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저 당 하나의 통계 값만 가질 수 있다.
                        name = "uk_user_id",
                        columnNames = {"user_id"}
                )
        }
)
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

    // 현재 연속 인증 기간의 시작일이다. 밑의 4개의 필드들은 전체 챌린지 수행 횟수를 기준으로 한다.
    @Column(name = "current_streak_start_date")
    private LocalDate currentStreakStartDate;

    // 현재 연속 인증 기간의 마지막 날이다.
    @Column(name = "current_streak_end_date")
    private LocalDate currentStreakEndDate;

    // 최대 연속 인증 기간의 시작일이다.
    @Column(name = "max_streak_start_date")
    private LocalDate maxStreakStartDate;

    // 최대 연속 인증 기간의 마지막 날이다.
    @Column(name = "max_streak_end_date")
    private LocalDate maxStreakEndDate;

    // 마지막 통계 갱신 시각이다.
    @LastModifiedDate
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Builder
    public UserStatistics(User user) {
        this.user = user;
        this.totalChallengeCount = 0;
        this.currentStreakStartDate = null;
        this.currentStreakEndDate = null;
        this.maxStreakStartDate = null;
        this.maxStreakEndDate = null;
    }

    /*
        챌린지 하나에 새로 참여했을 때 호출되는 함수이다.
     */
    public void increaseChallengeCount() {
        this.totalChallengeCount++;
    }

    /*
        하루에 챌린지를 하나라도 성공하면 update하는 함수이다.
     */
    public void recordDailySuccess(LocalDate today) {
        if (currentStreakEndDate == null ||  // 첫 인증인 경우이다.
                // 마지막으로 연속 인증한 날짜의 다음 날이 오늘이 아니라면 연속이 끊긴다.
                !currentStreakEndDate.plusDays(1).equals(today)) {
            currentStreakStartDate = today;
            currentStreakEndDate = today;
        }
        else {  // 어제까지 챌린지를 연속으로 성공한 경우엔, 마지막 수행일을 업데이트 한다.
            currentStreakEndDate = today;
        }

        // 최대 연속 기록 갱신 여부를 판단한다.
        if (maxStreakStartDate == null ||  // 처음으로 최대 연속 기록을 등록하는 경우이다.
                getCurrentStreakLength() > getMaxStreakLength()) {  // 연속 성공 일수가 최대보다 현재가 더 많은 경우이다.
            maxStreakStartDate = currentStreakStartDate;
            maxStreakEndDate = currentStreakEndDate;
        }
    }

    /*
        챌린지를 하지 않은 것이 확정됐을 때, 서비스에서 호출하는 함수이다.
     */
    public void recordDailyFail() {
        currentStreakStartDate = null;
        currentStreakEndDate = null;
    }

    /*
        현재 챌린지 연속 성공 일수를 계산해주는 함수이다.
     */
    public long getCurrentStreakLength() {
        if (currentStreakStartDate == null || currentStreakEndDate == null) {  // 챌린지 연속 인증이 끊긴 경우이다.
            return 0;
        }
        // 현재 챌린지 연속 성공 일수를 계산해서 리턴한다.
        return ChronoUnit.DAYS.between(currentStreakStartDate, currentStreakEndDate) + 1;
    }

    /*
        최대 챌린지 연속 성공 일수를 계산해주는 함수이다.
     */
    public long getMaxStreakLength() {
        if (maxStreakStartDate == null || maxStreakEndDate == null) {  // 챌린지를 연속으로 수행한적이 없는 경우이다.
            return 0;
        }
        // 최대 챌린지 연속 성공 일수를 계산해서 리턴한다.
        return ChronoUnit.DAYS.between(maxStreakStartDate, maxStreakEndDate) + 1;
    }
}
