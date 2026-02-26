// 정성우

package me.jsu.springbootdeveloper.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.post.repository.ReportArticleRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.CascadeType.REMOVE;

/*
    DB에 관리될 post에 관한 정보가 저장되는 클래스이다.

    쿼리의 성능 향상을 위해 post 날짜와 유저 id로 인덱싱을 한다.
    또한 한 챌린지 당 하나의 인증글만 하루에 올릴 수 있도록 유니크 설정을 한다.
 */
@Table(
        name = "posts",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저당 하루에 하나의 글만 올릴 수 있다.
                        name = "uk_user_challenge_date_article",
                        columnNames = {"user_id", "challenge_id", "post_date"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_user_date_article",
                        columnList = "user_id, post_date"  // 유저별 하루의 post를 검사하기 위한 인덱싱이다.
                ),
                @Index(
                        name = "idx_created_at",
                        columnList = "created_at"  // 최신순으로 post 피드를 보여주기 위한 인덱싱이다.
                ),
                @Index(
                        name = "idx_user_challenge_for_post",
                        columnList = "user_id, challenge_id"  // 유저 id와 챌린지 id로 인증글을 찾기 위한 인덱싱이다.
                ),
                @Index(
                        name = "idx_challenge_created_at",
                        columnList = "challenge_id, created_at desc"
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

    @Column(name = "title", nullable = true)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreatedDate  // 게시글이 생성된 시간이다.
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 사용자에게 보여주기 위한 게시글의 생성/수정 시각이다.
    @Column(name = "updated_at")
    private LocalDateTime updateAt;

    // user DB의 값을 갖는 fk값이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)  // 유저가 탈퇴해도 게시글은 남는다.
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = true)
    private Challenge challenge;

    // 서버 시각과는 다르게 인증글이 정확히 그 날짜에 올라왔는지 판단하기 위한 값이다.
    @Column(name = "post_date")
    private LocalDate postDate;

    @Column(name = "like_number")
    private long likeNumber;  // 게시글의 좋아요 수이다.

    @Column(name = "comment_number")
    private long commentNumber;  // 게시글의 댓글 수이다.

    @Column(name = "is_edited")
    private Boolean isEdited;  // 인증글의 수정 여부이다.

    @Column(name = "total_report")
    private Integer totalReport;  // 총 신고된 횟수이다.

    /*
        다음 코드는 CASCADE 설정으로 Article 엔티티를 fk값으로 가지고 있는 DB들을 자동으로 삭제해주는 설정이다.
        게시글 삭제 시, 밑의 정보들도 함께 전부 삭제된다.
     */
    @OneToMany(mappedBy = "article", cascade = REMOVE)
    private List<Comment> comments;

    @OneToMany(mappedBy = "article", cascade = REMOVE)
    private List<PostLike> postLikes;

    @OneToMany(mappedBy = "article", cascade = REMOVE)
    private List<PostPicture> postPictures;

    @OneToMany(mappedBy = "article", cascade = REMOVE)  // 인증글이 삭제되면 신고수도 자동으로 삭제된다.
    private List<ReportArticle> reportArticles;

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
    public Article(String content, User user, Challenge challenge, LocalDateTime updateAt, LocalDate postDate) {
        this.content = content;
        this.user = user;
        this.challenge = challenge;
        this.updateAt = updateAt;
        this.postDate = postDate;
        this.likeNumber = 0;  // 초기엔 둘 다 0이다.
        this.commentNumber = 0;
        this.isEdited = false;
        this.totalReport = 0;
    }

    /*
        내용을 업데이트 한다.
     */
    public void updateContent(String content, LocalDateTime updateAt) {
        if (content != null) {
            this.content = content;
        }
        this.updateAt = updateAt;
        this.isEdited = true;
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

    /*
        신고를 받은 경우, 신고 수를 증가시킨다.
     */
    public void plusTotalReport() {
        if (totalReport == null) {
            totalReport = 0;
        }

        totalReport++;
    }

    /*
        신고 처리를 완료한 경우, 신고 수를 감소시킨다.
     */
    public void minusTotalReport() {
        if (totalReport == null || totalReport == 0) {
            return;
        }

        totalReport--;
    }
}
