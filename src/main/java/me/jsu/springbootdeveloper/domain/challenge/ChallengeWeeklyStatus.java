// 정성우

package me.jsu.springbootdeveloper.domain.challenge;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    챌린지 별로 일주일 동안 해당 챌린지를 완수한 개수를 관리하는 클래스이다.
    챌린지 데일리 로그의 파생 데이터를 관리하는 클래스지만, 캐싱을 위해 사용한다.
    주에 한 번이라도 챌린지를 수행하면 해당 엔트리가 생성되지만, 한 번도 수행하지 않는다면 생성되지 않는다.
    그 경우엔 service에서 직접 일주일간 데일리 로그를 검색하여 챌린지 실패 여부를 판단한다.
 */
@Table(
        name = "challenge_weekly_status",
        uniqueConstraints = {
                @UniqueConstraint(  // 1주차 당 한 챌린지만 해당 엔티티 값이 존재하도록 보장한다.
                        name = "uk_challenge_user_week",
                        columnNames = {"challenge_id", "user_id", "week_start_date"}
                )
        },
        indexes = {
                @Index(  // 유저 id로 이번주 챌린지 리스트들을 검색하는 인덱싱이다.
                        name = "idx_user_id_weekly",
                        columnList = "user_id"
                )
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeWeeklyStatus {

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

    // ISO week 기준 시작 날짜(월요일 등으로 설정 가능)로 시작 날짜를 기준으로 일주일을 계산한다.
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    // 이번 주 성공 횟수를 저장한다.
    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", updatable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ChallengeWeeklyStatus(User user, Challenge challenge, LocalDate weekStartDate) {
        this.user = user;
        this.challenge = challenge;
        this.weekStartDate = weekStartDate;
        this.successCount = 1;  // 해당 엔트리가 생성되는 조건은 최소 한 번은 인증을 했을때 이므로 초기값은 1이다.
        this.status = Status.IN_PROGRESS;
    }

    /*
        post로 챌린지를 인증하면 주당 성공 횟수가 업데이트 되는 함수이다.
     */
    public void updateSuccessCount(int requiredCount) {  // 주당 최소 성공 횟수를 매개변수로 받는다.
        this.successCount++;

        if (this.successCount >= requiredCount) {  // 성공 횟수 업데이트와 동시에 주간 인증 성공여부를 판단한다.
            this.status = Status.SUCCESS;
        }
    }

    /*
        일주일이 지나 서비스에서 실패 처리할 때 사용되는 함수이다.
     */
    public void markFail() {
        this.status = Status.FAIL;
    }
}
