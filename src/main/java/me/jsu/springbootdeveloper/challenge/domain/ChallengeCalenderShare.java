// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.*;
import me.jsu.springbootdeveloper.challenge.enumType.ShareImageStatus;
import me.jsu.springbootdeveloper.user.domain.User;

import java.time.LocalDateTime;

/*
    사용자가 공유한 캘린더 사진을 저장하기 위한 도메인이다.
    공유하려는 날짜가 바뀔 때마다 사진을 업데이트 한다.
 */
@Entity
@Table(
        name = "challenge_share_image",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_share_image_owner_challenge_year_month",
                        columnNames = {"user_id", "challenge_id", "target_year", "target_month"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_expired_for_calender",
                        columnList = "expired_at, status, retry_count"
                ),
                @Index(
                        name = "idx_stuck_processing",
                        columnList = "status, updated_at, retry_count"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChallengeCalenderShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "target_year")
    private int year;

    @Column(name = "target_month")
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShareImageStatus status;

    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "retry_count")
    private Integer retryCount;  // 업로드 오류 시 재시도 한 횟수이다.

    // 마지막으로 사진을 업데이트 한 시간
    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    // 이미지가 만료되는 시간
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder
    public ChallengeCalenderShare(User user, Challenge challenge, int year, int month, LocalDateTime updateAt) {
        this.user = user;
        this.challenge = challenge;
        this.year = year;
        this.month = month;
        this.updateAt = updateAt;
        this.status = ShareImageStatus.PROCESSING;
        this.retryCount = 0;
    }

    public void markReady(String imageUrl, String publicId, LocalDateTime updateAt, LocalDateTime expiredAt) {
        this.status = ShareImageStatus.READY;
        this.imageUrl = imageUrl;
        this.cloudinaryPublicId = publicId;
        this.updateAt = updateAt;
        this.expiredAt = expiredAt;
    }

    public void markProcessing(int year, int month, LocalDateTime updateAt) {
        this.status = ShareImageStatus.PROCESSING;
        this.year = year;
        this.month = month;
        this.updateAt = updateAt;
    }

    /*
        클라우디너리 업로드에 실패한 경우, 재시도 횟수를 1 증가시킨다.
     */
    public void increaseRetryCount() {
        this.retryCount++;
    }
}
