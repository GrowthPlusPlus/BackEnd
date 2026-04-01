// 정성우

package me.jsu.springbootdeveloper.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.common.IntArrayConverter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    유저의 연간 활동 빈도인 잔디를 관리하는 도메인이다.
 */
@Table(
        name = "user_yearly_activity",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저 당 하나의 통계 값만 가질 수 있다.
                        name = "uk_user_id_year",
                        columnNames = {"user_id", "year"}
                )
        },
        indexes = {
                @Index(
                        name = "idx__user_id_for_yearly_activity",
                        columnList = "user_id")
        }
)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserYearlyActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 유저는 연 마다 하나의 통계를 가진다.
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "year", updatable = false)  // 잔디의 연도이다.
    private int year;

    @Convert(converter = IntArrayConverter.class)
    @Column(columnDefinition = "TEXT", name = "day_bit_count")  // 365 or 366일 동안의 날짜마다 하루 총 인증 횟수를 가진 배열이다.
    private int[] dayBitCount;

    // 잔디 통계 생성일이다.
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserYearlyActivity(User user, int year, int[] dayBitCount) {
        this.user = user;
        this.year = year;
        this.dayBitCount = dayBitCount;
    }

    /*
        유저의 잔디를 업데이트 하는 함수이다.
     */
    public void updateDayBitCount(int index, int count) {
        this.dayBitCount[index] = count;
    }

    /*
        관리자에 의해 과거 글이 삭제되어 로그도 삭제된 경우, 새로운 비트 배열로 바꾸는 함수이다.
     */
    public void replaceDayBitCount(int[] dayBitCount) {
        this.dayBitCount = dayBitCount;
    }
}
