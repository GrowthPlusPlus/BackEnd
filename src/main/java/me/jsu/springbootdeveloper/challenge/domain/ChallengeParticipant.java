// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Table(
        name = "challenge_participant",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저가 동일한 챌린지에 한 번만 참여하도록 유니크성을 부여한다.
                        name = "uk_user_challenge",
                        columnNames = {"user_id", "challenge_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_cp_user_id",
                        columnList = "user_id"),  // 유저 id로 검색하기 위한 인덱싱이다.
                @Index(
                        name = "idx_cp_challenge_id",
                        columnList = "challenge_id"),  // 챌린지 id로 검색하기 위한 인덱싱이다.
                @Index(
                        name = "idx_cp_user_id_status",
                        columnList = "user_id, status"),  // 유저 id, 챌린지 진행 여부로 검색하기 위한 인덱싱이다.
                @Index(
                        name = "idx__status",
                        columnList = "status"),
                @Index(
                        name = "idx_cp_challenge_score",  // 챌린지 id와 score로 빠르게 정렬하며 검색하기 위해 사용하는 인덱싱이다.
                        columnList = "challenge_id, ranking_score"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class ChallengeParticipant {

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

    @Column(name = "joined_date", nullable = false)
    private LocalDate joinDate;  // 유저가 챌린지에 참여한 시간이다.

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;  // 해당 튜플이 실제로 DB에 저장된 시간이다.

    @Column(name = "completed_at", nullable = true)
    private LocalDate completedAt;  // 완료된 날짜

    @Column(name = "failed_at", nullable = true)
    private LocalDate failedAt;  // 실패로 확정된 날짜

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;  // 현재 유저가 참여한 해당 챌린지의 상태이다. 진행중, 실패, 완료 상태가 있다.

    @Column(name = "total_success_days", nullable = false)
    private int totalSuccessDays;  // 해당 챌린지의 총 달성 일수이다.

    @Column(name = "current_streak_days", nullable = false)
    private int currentStreakDays;  // 해당 챌린지의 연속 달성 일수이다.

    // 연속 달성 일수를 계산하기 위해 사용한다.
    @Column(name = "current_streak_start_date")
    private LocalDate currentStreakStartDate;

    @Column(name = "current_streak_end_date")
    private LocalDate currentStreakEndDate;

    @Column(name = "max_streak_days", nullable = false)
    private int maxStreakDays;  // 해당 챌린지의 최고 연속 달성 일수이다.

    // 연속 최고 달성 일수를 계산하기 위해 사용한다.
    @Column(name = "max_streak_start_date")
    private LocalDate maxStreakStartDate;

    @Column(name = "max_streak_end_date")
    private LocalDate maxStreakEndDate;

    // 유저 기준 총 수행 주차 수
    @Column(name = "total_weeks", nullable = false)
    private int totalWeeks;

    // 성공한 주차 수
    @Column(name = "success_weeks", nullable = false)
    private int successWeeks;

    // 해당 챌린지의 랭킹 점수
    @Column(name = "ranking_score")
    private Double rankingScore = 0.0;

    @Builder
    public ChallengeParticipant(Challenge challenge, User user, LocalDate joinDate, int totalWeeks) {
        this.challenge = challenge;
        this.user = user;
        this.joinDate = joinDate;
        this.status = Status.IN_PROGRESS;  // 처음 챌린지에 참여하면 진행중인 상태가 된다.
        this.totalSuccessDays = 0;  // 초기엔 전부 0이다.
        this.currentStreakDays = 0;
        this.maxStreakDays = 0;
        this.successWeeks = 0;
        this.totalWeeks = totalWeeks;
    }

    /*
        해당 챌린지의 달성 일수, 연속 일수, 최대 연속 일수를 업데이트 하는 함수로, 하루에 한 번 인증글을 올렸을 때 수행된다.
        하루에 무조건 한 번만 수행 되어야 한다.
     */
    public void recordDailySuccess(LocalDate today) {
        // 총 완료 일수를 증가시킨다.
        totalSuccessDays++;

        // 현재 연속 달성 일수를 처리한다.
        if (currentStreakEndDate == null ||  // 첫 성공인 경우이다.
                !currentStreakEndDate.plusDays(1).equals(today)) {  // 연속이 끊긴 경우이다.
            currentStreakStartDate = today;  // 날짜를 초기화 한다.
            currentStreakEndDate = today;
            currentStreakDays = 1;
        } else {  // 연속 성공 중인 경우이다.
            currentStreakEndDate = today;
            currentStreakDays++;
        }

        // 최대 연속 달성 일수를 처리한다.
        if (maxStreakStartDate == null || currentStreakDays > maxStreakDays) {  // 처음이거나, 기존 연속 기록보다 높아진 경우이다.
            maxStreakStartDate = currentStreakStartDate;  // 현재 연속 달성 일수를 최대 인증 달성 횟수에 대입한다.
            maxStreakEndDate = currentStreakEndDate;
            maxStreakDays = currentStreakDays;
        }
    }

    /*
        인증글을 삭제했을 경우 챌린지의 총 달성일수와 연속 달성일수, 최대 달성일수를 롤백하는 함수이다.
        현재는 최대 달성일수가 현재 연속 달성 일수와 같았을 경우 돌아갈 수 없기 때문에 무효화 한다.
     */
    public void removeDailySuccess(LocalDate today) {
        // 전체 성공 일수를 감소시킨다.
        totalSuccessDays--;

        // 현재 연속 달성 기록을 처리한다. 첫 if문은 과거글이 삭제되었을 때 수행하지 않도록 하는 방어코드다.
        if (currentStreakEndDate != null && currentStreakEndDate.equals(today)) {  // 연속 기록을 달성한적이 있고, 그게 오늘이라면 초기화 한다.
            if (currentStreakDays == 1) {  // 연속 기록이 1일짜리였다면 완전히 초기화 한다.
                currentStreakStartDate = null;
                currentStreakEndDate = null;
                currentStreakDays = 0;
            } else {
                // 연속 기록이 이어진 상태였다면 하루 줄인다
                currentStreakEndDate = today.minusDays(1);
                currentStreakDays--;
            }
        }

        // 최대 연속 기록을 처리한다.
        if (maxStreakEndDate != null && maxStreakEndDate.equals(today)) {  // 오늘 삭제한 연속일수가 최대기록의 마지막 날인지 확인한다.
            if (maxStreakDays == 1) {  // 최대 연속 일수가 하루였다면 초기화 한다.ㅊ
                maxStreakStartDate = null;
                maxStreakEndDate = null;
                maxStreakDays = 0;
            }
            else {  // 최대 연속 일수를 하루 줄인다.
                maxStreakEndDate = today.minusDays(1);
                maxStreakDays--;
            }
        }
    }

    /*
        인증글을 연속으로 올리지 않았을 경우, 현재 연속 일수를 초기화 하는 함수이다.
     */
    public void initCurrentStreakDays() {
        currentStreakStartDate = null;
        currentStreakEndDate = null;
        currentStreakDays = 0;
    }

    /*
        챌린지가 성공한 경우 실행하는 함수이다.
     */
    public void markSuccess(LocalDate today) {
        this.status = Status.SUCCESS;
        this.completedAt = today;
    }

    /*
        챌린지가 실패한 경우 실행하는 함수이다.
     */
    public void markFail(LocalDate today) {
        this.status = Status.FAIL;
        this.failedAt = today;
    }

    /*
        챌린지의 총 성공 주차 수를 업데이트 한다.
     */
    public void increaseSuccessWeeks() {
        successWeeks++;
    }

    /*
        챌린지의 총 주차수와 성공 주차수를 비교하여 주간 실패율을 계산한다.
        실패한 챌린지가 아니라면 의미 없는 값이 반환된다.
     */
    public double getFailureRateByWeek() {
        if (totalWeeks == 0) {  // 0으로 나누는 것을 방지하기 위해 사용한다.
            return 0.0;
        }

        int failedWeeks = Math.max(0, totalWeeks - successWeeks);
        return (double) failedWeeks / (double) totalWeeks;
    }

    /*
        사용자의 랭킹 점수를 업데이트 한다.
     */
    public void updateRankingScore(double score) {
        this.rankingScore = score;
    }
}
