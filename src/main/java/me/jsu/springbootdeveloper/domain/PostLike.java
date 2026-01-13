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

/*
    게시글의 좋아요를 관리하는 도메인 클래스이다.
 */
@Table(
        name = "post_like",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저는 게시글에 하나의 좋아요만 가능하다. 또한 유저가 좋아요를 누른 여부를 확인하기 위한 인덱싱도 있다.
                        name = "uk_user_article",
                        columnNames = {"user_id", "article_id"}
                )
        },
        indexes = {
                @Index(  // 게시글에 좋아요를 누른 유저를 보기 위한 인덱스이다.
                        name = "idx_article",
                        columnList = "article_id"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 한 유저가 여러 게시글에 좋아요가 가능하므로 many to one 관계이다.
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)  // 한 게시글에 여러 좋아요가 가능하므로 many to one 관계이다.
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public PostLike(User user, Article article) {
        this.user = user;
        this.article = article;
    }
}
