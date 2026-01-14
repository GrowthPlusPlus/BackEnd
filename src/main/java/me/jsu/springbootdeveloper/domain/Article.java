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
        uniqueConstraints = {
                @UniqueConstraint(  // 유저당 하루에 하나의 글만 올릴 수 있다.
                        name = "uk_user_challenge_date",
                        columnNames = {"user_id", "challenge_id", "post_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_date",
                        columnList = "user_id, post_date"  // 유저별 하루의 post를 검사하기 위한 인덱싱이다.
                ),
                @Index(
                        name = "idx_created_at",
                        columnList = "created_at"  // 최신순으로 post 피드를 보여주기 위한 인덱싱이다.
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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreatedDate  // 게시글이 생성된 시간이다.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate  // 게시글이 수정된 시간이다. 필드를 수정하면 자동으로 업데이트 된다.
    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    // user DB의 값을 갖는 fk값이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    // 서버 시각과는 다르게 인증글이 정확히 그 날짜에 올라왔는지 판단하기 위한 값이다.
    @Column(name = "post_date")
    private LocalDate postDate;

    @Column(name = "like_number")
    private int likeNumber;  // 게시글의 좋아요 수이다.

    @Column(name = "comment_number")
    private int commentNumber;  // 게시글의 댓글 수이다.

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
    public Article(String title, String content, User user, Challenge challenge, LocalDate postDate) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.challenge = challenge;
        this.postDate = postDate;
        this.likeNumber = 0;  // 초기엔 둘 다 0이다.
        this.commentNumber = 0;
    }

    public void update(String title, String content) {
        if (title != null) {  // 게시글을 업데이트하는 경우, 내용만 수정해도 제목에는 null값이 들어가지 않도록 한다.
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    /*
        좋아요를 누른 경우 좋아요 개수를 증가시킨다. 중복 좋아요는 서비스 레이어에서 방지한다.
     */
    public void plusLikeNumber() {
        likeNumber++;
    }

    /*
        좋아요를 취소한 경우 좋아요 개수를 감소시킨다.
     */
    public void minusLikeNumber() {
        likeNumber--;
    }

    /*
        댓글이 달린 경우 댓글 수를 증가시킨다.
     */
    public void plusCommentNumber() {
        commentNumber++;
    }

    /*
        댓글이 삭제된 경우 댓글 수를 감소시킨다.
     */
    public void minusCommentNumber() {
        commentNumber--;
    }
}
