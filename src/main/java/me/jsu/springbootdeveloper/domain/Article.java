// 정성우

package me.jsu.springbootdeveloper.domain;

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

/*
    DB에 관리될 post에 관한 정보가 저장되는 클래스이다.

    쿼리의 성능 향상을 위해 post 날짜와 유저 id로 인덱싱을 한다.
    또한 한 챌린지 당 하나의 인증글만 하루에 올릴 수 있도록 유니크 설정을 한다.
 */
@Table(
        name = "posts",
        indexes = {
                @Index(
                        name = "uk_user_challenge_date",
                        columnList = "user_id, challenge_id, post_date",
                        unique = true
                ),
                @Index(
                        name = "idx_user_date",
                        columnList = "user_id, post_date"
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @CreatedDate  // 게시글이 생성된 시간이다.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate  // 게시글이 수정된 시간이다.
    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    // 서버 시각과는 다르게 인증글이 정확히 그 날짜에 올라왔는지 판단하기 위한 값이다.
    @Column(name = "post_date")
    private LocalDate postDate;

    /*
        @Builder로 빌더 패턴으로 객체를 생성할 수 있게 해준다.
        ex)
        Article article = Article.builder()
            .title("제목")
            .content("내용")
            .author("홍길동")
            .build();
     */
    @Builder
    public Article(String title, String content, User user, Challenge challenge, LocalDate postDate, LocalDateTime createdAt) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.challenge = challenge;
        this.postDate = postDate;
        this.createdAt = createdAt;  // 게시글 생성 시각이다.
        this.updateAt = createdAt;  // 처음엔 게시글 생성 시각과 같다.
    }

    public void update(String title, String content, LocalDateTime updateAt) {
        if (title != null) {  // 게시글을 업데이트하는 경우, 내용만 수정해도 제목에는 null값이 들어가지 않도록 한다.
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }

        this.updateAt = updateAt;  // 마지막 수정 시각을 업데이트 한다.
    }
}
