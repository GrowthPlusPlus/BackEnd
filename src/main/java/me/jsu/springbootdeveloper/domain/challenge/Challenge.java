// 정성우

package me.jsu.springbootdeveloper.domain.challenge;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    DB에 저장될 챌린지 정보이다.
 */
@Table(
        name = "challenge",
        indexes = {
                @Index(  // 피드를 위해 공개 범위와 시작 일로 최신순으로 인덱싱 한다.
                        name = "idx_visibility_start_date",
                        columnList = "challenge_visibility, start_date"
                ),
                @Index(  // 내가 만든 챌린지를 관리하기 위해 내 id로 인덱싱 한다.
                        name = "idx_user_created",
                        columnList = "user_id"
                ),
                /*
                    챌린지 시작일과 종료일로 인덱싱 하여 진행중, 완료, 종료 상태의 챌린지를 쉽게 찾아낸다.
                    참여자는 챌린지 참여자 DB에서 상태를 알아낼 수 있으므로, 챌린지에 참여하지 않은 유저를 위한 인덱싱이다.
                 */
                @Index(
                        name = "idx_date_range",
                        columnList = "start_date, end_date"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    // user DB의 값을 갖는 fk값으로, 챌린지를 생성한 유저의 id를 나타낸다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "maxParticipantNumber", nullable = false)
    private int maxParticipantNumber;

    // 현재 참여하고 있는 참가자 수이다. 파생 데이터지만 캐싱을 위해 사용한다
    @Column(name = "participantNumber", nullable = false)
    private int participantNumber;

    @Enumerated(EnumType.STRING)
    private ChallengeVisibility challengeVisibility;  // 챌린지 공개 범위를 설정하기 위한 값이다.

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;  // 챌린지 시작일이다.

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;  // 챌린지 종료일이다.

    @Column(name = "requiredWeeklyCount", nullable = false)
    private int requiredWeeklyCount;  // 주당 최소 인증 횟수이다.

    @Column(name = "photo_required", nullable = false)
    private boolean photoRequired;  // 사진 여부가 필수인지 정하는 값이다.

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Challenge(String title, String content, User user, int maxParticipantNumber, ChallengeVisibility challengeVisibility, 
                     LocalDate startDate, LocalDate endDate, int requiredWeeklyCount, boolean photoRequired) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.maxParticipantNumber = maxParticipantNumber;
        this.participantNumber = 1;  // 초기 상태엔 혼자만 있다.
        this.challengeVisibility = challengeVisibility;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requiredWeeklyCount = requiredWeeklyCount;
        this.photoRequired = photoRequired;
    }

    /*
        챌린지의 내용을 업데이트하는 함수이다.
        챌린지의 이름, 마스터 유저, 최대 참여자 수, 참여자 수를 제외하고 수정이 가능하다.
     */
    public void update(String content, ChallengeVisibility challengeVisibility) {
        if (content != null) {
            this.content = content;
        }

        if (challengeVisibility != null) {
            this.challengeVisibility = challengeVisibility;
        }
    }

    /*
        챌린지의 오너를 위임하는 함수이다.
     */
    public void updateUser(User user) {
        this.user = user;
    }

    /*
        챌린지에 참여자 수가 늘 때마다 참여자 필드 값을 업데이트 한다.
     */
    public void updateParticipate() {
        this.participantNumber++;
    }

    /*
        챌린지에 참여자 수가 줄 때마다 참여자 필드 값을 업데이트 한다.
     */
    public void updateLeave() {
        if (this.participantNumber <= 0) {
            throw new IllegalStateException("Participant number corrupted for challenge id=" + id);
        }

        this.participantNumber--;
    }
}
