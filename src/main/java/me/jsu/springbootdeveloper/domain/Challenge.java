// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    DB에 저장될 챌린지 정보이다.
 */
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
    @JoinColumn(name = "user_id")
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
    private ChallengeVisibility challengeVisibility;  // 유저 별 권한 지정을 위한 enum이다.

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Challenge(String title, String content, User user, int maxParticipantNumber, ChallengeVisibility challengeVisibility) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.maxParticipantNumber = maxParticipantNumber;
        this.participantNumber = 1;  // 초기 상태엔 혼자만 있다.
        this.challengeVisibility = challengeVisibility;
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
        챌린지에 참여자 수가 늘 때마다 참여자 필드 값을 업데이트 한다.
     */
    public void updateParticipantNumber() {
        this.participantNumber++;
    }
}
