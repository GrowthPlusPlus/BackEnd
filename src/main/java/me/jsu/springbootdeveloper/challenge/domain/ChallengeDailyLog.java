// 정성우

package me.jsu.springbootdeveloper.challenge.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.post.domain.Article;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

/*
    DB에 저장될 챌린지 데일리 로그 정보이다.
    post로 챌린지를 인증할 때마다 업데이트 되며, 해당 정보로 사용자의 통계를 내는 등 핵심적인 요소이다.
 */
@Table(
        name = "challengeDailyLog",
        uniqueConstraints = {
                @UniqueConstraint(  // 한 챌린지 당 post가 하나만 가능하므로, 하루에 한 로그만 생성된다.
                        name = "uk_user_challenge_date_log",
                        columnNames = {"user_id", "challenge_id", "post_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_date_log",
                        columnList = "user_id, post_date"  // 조회용 인덱스로, 유저 id와 게시 날짜를 포함하여 검색하면 성능이 향상된다.
                ),
                @Index(
                        name = "idx_user_challenge_log",
                        columnList = "user_id, challenge_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class ChallengeDailyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)  // 유저가 계정을 탈퇴해도 로그 값은 남는다.
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = true)
    private Challenge challenge;

    @OneToOne(fetch = FetchType.LAZY)  // 하루에 한 게시글 당 하나의 로그만 생성되므로 OneToOne 관계이다.
    @JoinColumn(name = "article_id", unique = true)
    private Article article;

    // 해당 날에 챌린지를 수행했는지 안했는지 체크하기 위해 사용하는 값이다.
    @Column(name = "post_date", nullable = false)
    private LocalDate postDate;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public ChallengeDailyLog(User user, Challenge challenge, Article article, LocalDate postDate) {
        this.user = user;
        this.challenge = challenge;
        this.article = article;
        this.postDate = postDate;
    }
}
