// 정성우

package me.jsu.springbootdeveloper.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @JoinColumn(name = "user_id", unique = true, nullable = true)  // 유저가 탈퇴해도 통계는 남는다.
    private User user;

    /* =========================
       [성과 요약] 완료/실패 챌린지 수
       ========================= */
    @Column(name = "total_challenge_count", nullable = false)
    private int totalChallengeCount;  // 참여한 전체 챌린지 수이다.

    @Column(name = "completed_challenge_count", nullable = false)
    private int completedChallengeCount;  // 완료한 챌린지 수이다.

    @Column(name = "failed_challenge_count", nullable = false)
    private int failedChallengeCount;  // 실패한 챌린지 수이다.

    /* =========================
       [유저 단위 연속성] 유저가 하루라도 활동했는지가 기준이다. (챌린지 별 연속성 X)
       ========================= */
    @Column(name = "current_streak_start_date")
    private LocalDate currentStreakStartDate;  // 연속 인증 기간의 시작일이다.

    @Column(name = "current_streak_end_date")
    private LocalDate currentStreakEndDate;  // 연속 인증 기간의 마지막 날이다.

    @Column(name = "max_streak_start_date")
    private LocalDate maxStreakStartDate;  // 최대 연속 인증 기간의 시작일이다.

    @Column(name = "max_streak_end_date")
    private LocalDate maxStreakEndDate;  // 최대 연속 인증 기간의 마지막 날이다.

    /* =========================
       [활동 빈도 요약] (활동한 날 기준 / 슬라이딩 윈도우)
       - 최근 30일을 bitset으로 저장한다. (1이면 그 날 활동)
       - weeklyActiveDays(최근 7일), monthlyActiveDays(최근 30일)는 bits로부터 파생 계산한다.
       ========================= */
    @Column(name = "total_active_days", nullable = false)
    private int totalActiveDays;  // 유저가 총 활동한 날짜 수이다.

    @Column(name = "activity_window_start", nullable = false)
    private LocalDate activityWindowStart;  // 이 날짜를 bits의 "0번째(LSB)"로 본다. 즉 오늘이다.

    @Column(name = "activity_bits", nullable = false)
    private long activityBits;  // 최근 30일(정확히는 0~29일)을 비트로 표현

    @Column(name = "last_active_date")
    private LocalDate lastActiveDate;  // "마지막으로 활동한 날" (당일 취소 판단에 사용)

    /* =========================
       [대표 챌린지: 수행 빈도] (일별 업데이트)
       - dominantScore = 해당 챌린지의 totalSuccessDays(=성공한 날짜 수)
       ========================= */
    @Column(name = "dominant_challenge_id")
    private Long dominantChallengeId;  // 대표 챌린지의 id이다. 가장 성공한 날짜 수가 높은 챌린지가 기준이 된다.

    @Column(name = "dominant_challenge_score", nullable = false)
    private int dominantChallengeScore;

    /* =========================
       [대표 챌린지: 주간 실패] (주간 정산에서만 업데이트)
       주기적으로 초기화 된다.
       - failureRate = failedWeeks / evaluatedWeeks
       ========================= */
    @Column(name = "most_failed_challenge_id")
    private Long mostFailedChallengeId;  // 오래 지속하지 못하고 실패한 챌린지 id

    @Column(name = "most_failed_failure_rate", nullable = false)
    private double mostFailedChallengeFailureRate;  // 실패율

    @Column(name = "most_failed_updated_at")
    private LocalDate mostFailedUpdatedAt;  // 실패 챌린지가 업데이트 된 날짜이다.

    /* =========================
       [대표 챌린지: 불안정(variance)] (주간 정산에서만 업데이트)
       - variance = 주간 성공 횟수(weekly successCount)의 분산으로, 주를 기준으로 수행 패턴의 변동성을 확인한다. 
       ========================= */ 
    @Column(name = "most_unstable_challenge_id")
    private Long mostUnstableChallengeId;  // 챌린지 수행 횟수가 불규칙적인 챌린지 id

    @Column(name = "most_unstable_variance", nullable = false)
    private double mostUnstableChallengeVariance;  // 주간 성공 횟수의 분산

    // 통계 생성일이다.
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserStatistics(User user) {
        this.user = user;
        this.totalChallengeCount = 0;

        this.completedChallengeCount = 0;
        this.failedChallengeCount = 0;

        this.currentStreakStartDate = null;
        this.currentStreakEndDate = null;
        this.maxStreakStartDate = null;
        this.maxStreakEndDate = null;

        this.totalActiveDays = 0;
        this.activityWindowStart = LocalDate.now(); // 초기값(생성 시점)
        this.activityBits = 0L;
        this.lastActiveDate = null;

        this.dominantChallengeId = null;
        this.dominantChallengeScore = 0;

        this.mostFailedChallengeId = null;
        this.mostFailedChallengeFailureRate = 0.0;
        this.mostFailedUpdatedAt = null;

        this.mostUnstableChallengeId = null;
        this.mostUnstableChallengeVariance = 0.0;
    }

    /*
        챌린지 하나에 새로 참여했을 때 호출되는 함수이다.
     */
    public void increaseChallengeCount() {
        this.totalChallengeCount++;
    }

    /*
        챌린지에서 나갔을 때 호출되는 함수이다.
     */
    public void decreaseChallengeCount() {
        this.totalChallengeCount--;
    }

    /* =========================
       [완료/실패 챌린지 수 갱신]
       ========================= */
    public void increaseCompletedChallengeCount() {
        this.completedChallengeCount++;
    }

    public void increaseFailedChallengeCount() {
        this.failedChallengeCount++;
    }

    /*
        하루에 챌린지를 하나라도 성공하면 유저 단위 연속 일수를 갱신하는 함수이다.
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
        인증글을 삭제하여 연속 일수가 초기화 되는 경우이다.
     */
    public void removeDailySuccess(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);

        if (currentStreakEndDate != null && !currentStreakStartDate.isAfter(yesterday)) {  // 연속 기록이 이어진 상태였다면 하루 줄인다
            currentStreakEndDate = yesterday;
        }
        else {  // 연속 기록이 1일짜리였다면 완전히 초기화 한다.
            currentStreakStartDate = null;
            currentStreakEndDate = null;
        }

        // 최대 연속 기록을 처리한다.
        if (maxStreakEndDate != null && maxStreakEndDate.equals(today)) {  // 오늘 삭제한 연속일수가 최대기록의 마지막 날인지 확인한다.
            LocalDate maxYesterday = today.minusDays(1);

            if (maxStreakStartDate != null && !maxStreakStartDate.isAfter(maxYesterday)) {
                maxStreakEndDate = maxYesterday;  // 어제로 날짜를 되돌린다.
            }
            else {
                // maxStreak이 오늘 하루짜리였던 경우이다.
                maxStreakStartDate = null;
                maxStreakEndDate = null;
            }
        }
    }

    /*
        챌린지를 하지 않은 것이 확정됐을 때, 연속 일수를 초기화 한다.
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

    /* =========================
        [활동일수 슬라이딩 윈도우]
        - bits는 "최근 30일"을 표현한다.
        - today 기준으로 윈도우를 이동(shift)한 뒤 오늘 활동 여부를 set/unset 한다.
        - 업데이트 한 날짜만 bit에 1을 추가하므로 공백 만큼을 diff로 계산한다.
        ========================= */
    // today가 윈도우 밖으로 나가면 윈도우를 today 기준으로 당겨준다.
    private void alignWindow(LocalDate today) {
        long diff = ChronoUnit.DAYS.between(activityWindowStart, today);  // 마지막 인증일과의 날짜 간격을 계산한다.
        if (diff <= 0) {
            return;  // 오늘 기준으로 윈도우를 이미 정렬했다면 윈도우를 움직이지 않는다. (과거 업데이트는 허용하지 않는다고 가정)
        }

        if (diff >= 30) {
            // 30일 이상 인증을 하지 않으면 기존 윈도우 의미가 사라지므로 초기화한다.
            activityBits = 0L;
        } else {
            // 날짜가 앞으로 가면 비트를 그만큼 왼쪽으로 이동 (오래된 날짜는 밀려나며 제거)
            activityBits <<= diff;  // 비트를 0으로 채운다.
            activityBits &= ((1L << 30) - 1); // 30비트만 유지한다.
        }

        activityWindowStart = today;  // 윈도우 기준일을 오늘로 바꾼다.
    }

    // 오늘 활동을 처리한다. (하루에 최소 1개 챌린지 인증 성공)
    public void markActiveToday(LocalDate today) {
        alignWindow(today);  // 윈도우를 정렬한다.

        // 오늘 비트(LSB)를 1로 설정한다. 즉 수행했음을 표시한다.
        long before = activityBits;  // 오늘 처리하기 전의 상태이다.
        activityBits |= 1L;

        // 오늘 처음으로 active가 된 경우만 totalActiveDays 증가
        if ((before & 1L) == 0L) {
            totalActiveDays++;
        }

        lastActiveDate = today;  // 마지막 인증일을 오늘로 바꾼다.
    }

    // 당일 취소로 인해 "오늘 활동이 0이 됨"이 확정되면 호출한다.
    public void unmarkActiveToday(LocalDate today) {
        alignWindow(today);

        // 오늘 비트(LSB)를 0으로 설정한다. 즉 안한 것으로 바꾼다.
        long before = activityBits;
        activityBits &= ~1L;

        // 오늘이 active였다가 inactive가 된 경우만 totalActiveDays 감소한다.
        if ((before & 1L) == 1L) {
            totalActiveDays--;
        }

        // lastActiveDate는 정확 복원이 어려우므로 오늘이였다면 null로 처리한다. (정확 복원은 별도 재계산 필요)
        if (lastActiveDate != null && lastActiveDate.equals(today)) {
            lastActiveDate = null;
        }
    }

    // 최근 7일 활동일수(파생)를 리턴한다.
    public int getWeeklyActiveDays() {
        long mask7 = (1L << 7) - 1;  // 7bit만 검사한다.
        return Long.bitCount(activityBits & mask7);
    }

    // 최근 30일 활동일수(파생)를 리턴한다.
    public int getMonthlyActiveDays() {
        long mask30 = (1L << 30) - 1;  // 30bit 모두 검사한다.
        return Long.bitCount(activityBits & mask30);
    }

    /* =========================
       [대표 챌린지 후보 비교] - 수행 빈도 대표(dominant)를 계산한다.
       newScore는 해당 챌린지의 총 수행 횟수이다.
       ========================= */
    public void considerDominantChallenge(Long challengeId, int newScore) {
        if (challengeId == null) {  // 비교할 챌린지가 없는 경우의 방어 코드이다.
            return;
        }

        // 수행 빈도가 기존 챌린지보다 더 높은 경우, 대표 챌린지를 변경한다.
        if (dominantChallengeId == null || newScore > dominantChallengeScore) {
            dominantChallengeId = challengeId;
            dominantChallengeScore = newScore;
        }
    }

    // 대표 챌린지를 설정하는 함수이다.
    public void setDominantChallenge(Long challengeId, int score) {
        this.dominantChallengeId = challengeId;
        this.dominantChallengeScore = score;
    }

    /*
        ChallengeDailyLog에 남아 있는 활동 날짜 목록으로, 유저 전역 연속·최대 연속·활동 비트·총 활동일을 다시 맞춘다.
        관리자가 과거 인증글을 삭제한 뒤 onTodayPostCancelled 대신 호출한다.

        @param sortedDistinctPostDates 오름차순 정렬된, 챌린지 연동 데일리 로그 기준 활동일 목록
        @param today 오늘(활동 비트 윈도우 기준일)
     */
    public void rebuildGlobalStreakAndActivityFromDistinctDates(List<LocalDate> sortedDistinctPostDates, LocalDate today) {
        if (sortedDistinctPostDates == null || sortedDistinctPostDates.isEmpty()) {  // 로그가 비어있는 경우 통계를 모두 초기화 한다.
            this.currentStreakStartDate = null;  // 연속 일수 초기화
            this.currentStreakEndDate = null;
            this.maxStreakStartDate = null;  // 최대 연속 일수 초기화
            this.maxStreakEndDate = null;
            this.totalActiveDays = 0;  // 총 수행 일수 초기화
            this.activityWindowStart = today != null ? today : LocalDate.now(ZoneId.of("Asia/Seoul"));  // 윈도우 시작일을 오늘로 초기화
            this.activityBits = 0L;
            this.lastActiveDate = null;  // 마지막 수행일 초기화

            return;
        }

        today = today != null ? today : LocalDate.now(ZoneId.of("Asia/Seoul"));  // today가 null일 경우를 대비한다.

        this.totalActiveDays = sortedDistinctPostDates.size();  // 총 챌린지 수행 일수는 로그 사이즈로 초기화 한다.
        this.lastActiveDate = sortedDistinctPostDates.get(sortedDistinctPostDates.size() - 1);  // 마지막 수행일을 로그의 맨 마지막 인덱스의 값으로 한다.

        // 현재 연속 수행일수를 구한다. 마지막 활동일을 끝으로 하는 가장 긴 연속 구간이다.
        LocalDate last = sortedDistinctPostDates.get(sortedDistinctPostDates.size() - 1);  // 현재 연속 수행일수의 마지막 날은 로그 마지막 기록일로 한다.
        LocalDate curStart = last;  // 연속 수행 일수의 시작일은 처음은 마지막 날로 한다.
        for (int i = sortedDistinctPostDates.size() - 2; i >= 0; i--) {
            if (sortedDistinctPostDates.get(i).plusDays(1).equals(sortedDistinctPostDates.get(i + 1))) {
                curStart = sortedDistinctPostDates.get(i);  // 맨 위부터 로그가 끊기지 않는 시점까지 현재 연속 수행 일수의 시작일을 찾는다.
            }
            else {
                break;  // 끊기면 탈출한다.
            }
        }
        this.currentStreakStartDate = curStart;  // 초기화
        this.currentStreakEndDate = last;

        // 최대 연속 수행일수를 구한다. 각 구간 길이를 비교하여 가장 긴 구간을 저장한다.
        int maxLen = 0;
        LocalDate bestStart = null;
        LocalDate bestEnd = null;
        int seg = 0;
        while (seg < sortedDistinctPostDates.size()) {
            int end = seg;
            while (end + 1 < sortedDistinctPostDates.size()
                    && sortedDistinctPostDates.get(end).plusDays(1).equals(sortedDistinctPostDates.get(end + 1))) {
                end++;
            }
            int len = end - seg + 1;
            if (len > maxLen) {
                maxLen = len;
                bestStart = sortedDistinctPostDates.get(seg);
                bestEnd = sortedDistinctPostDates.get(end);
            }
            seg = end + 1;
        }
        this.maxStreakStartDate = bestStart;
        this.maxStreakEndDate = bestEnd;

        // 최근 30일 비트를 계산한다. LSB가 today이고, i일 전에 활동이 있으면 i번째 비트를 켠다.
        Set<LocalDate> activeSet = new HashSet<>(sortedDistinctPostDates);
        this.activityWindowStart = today;  // 윈도우의 첫 날을 오늘로 설정한다.
        long bits = 0L;
        for (int i = 0; i < 30; i++) {
            LocalDate d = today.minusDays(i);
            if (activeSet.contains(d)) {  // 해당 날짜에 활동이 있다면 비트를 1로 바꾼다.
                bits |= (1L << i);
            }
        }
        this.activityBits = bits;  // 계산한 비트를 할당한다.
    }

    /* =========================
       [주간 대표 갱신] - mostFailed / mostUnstable
       주가 지날 때 마다 실패한 주의 비율이 높은 챌린지, 주마다 활동이 불규칙적인 챌린지를 업데이트 한다.
       ========================= */
    public void updateMostFailed(Long challengeId, double failureRate, LocalDate today) {
        this.mostFailedChallengeId = challengeId;
        this.mostFailedChallengeFailureRate = failureRate;  // 실패율
        this.mostFailedUpdatedAt = today;  // 대표 실패 챌린지의 업데이트 시간을 기록한다.
    }

    // 주마다 활동이 불규칙적인 챌린지를 업데이트 한다.
    public void updateMostUnstable(Long challengeId, double variance) {
        this.mostUnstableChallengeId = challengeId;
        this.mostUnstableChallengeVariance = variance;  // 활동 비율
    }
}
