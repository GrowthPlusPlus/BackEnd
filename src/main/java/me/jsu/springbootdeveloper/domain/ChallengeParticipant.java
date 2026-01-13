// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
                        columnList = "challenge_id")  // 챌린지 id로 검색하기 위한 인덱싱이다.
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

    @CreatedDate
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;  // 유저가 챌린지에 참여한 시간이다.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;  // 현재 유저가 참여한 해당 챌린지의 상태이다. 진행중, 실패, 완료 상태가 있다.

    @Builder
    public ChallengeParticipant(Challenge challenge, User user) {
        this.challenge = challenge;
        this.user = user;
        this.status = Status.IN_PROGRESS;  // 처음 챌린지에 참여하면 진행중인 상태가 된다.
    }
}
