// 정성우

package me.jsu.springbootdeveloper.domain.post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
    게시글에 달리는 댓글에 대한 정보를 가진 도메인 클래스이다.
    현재는 답글 쓰기 기능을 구현하지 않으므로 parent, children 부분은 사용되지 않는다.
 */
@Table(
        name = "comment",
        indexes = {
                @Index(
                        name = "idx_article_comment",
                        columnList = "article_id"  // 게시글에 달린 댓글을 검색하기 위한 인덱싱이다.
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // protected의 기본 생성자를 만들어준다.
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)  // 한 유저가 여러 댓글을 달 수 있으므로 many to one 관계이다.
    @JoinColumn(name = "user_id", nullable = true)  // 유저가 탈퇴해도 댓글은 남는다.
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)  // 한 게시글에 여러 댓글이 가능하므로 many to one 관계이다.
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;  // 해당 댓글의 부모 댓글이다. 게시글 자체에 댓글을 단 경우 부모가 없으므로 null 값이 된다.

    /*
        해당 맴버 변수는 이 댓글이 가진 자식 댓글의 값을 가진다. 즉 부모 댓글로 자식 댓글을 관리한다.
        mappedBy로 자식 댓글과 부모 댓글을 연결한다.
        cascade = CascadeType.ALL 옵션으로 부모 댓글에 수행되는 작업이 자식 댓글에도 영향이 간다. 즉 부모 댓글이 삭제되면 답글도 삭제된다.
        orphanRemoval = true 옵션은 부모 댓글이 삭제되면 자식 댓글도 자동으로 삭제해주는 옵션이다.

        현재는 답글 쓰기 기능이 없으므로 parent, children 필드는 사용되지 않는다.
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)  // varchar(255)보다 더 많은 내용이 들어갈 수 있다.
    private String contents;  // 댓글 내용이다.

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Builder
    public Comment(User user, Article article, String contents) {
        this.user = user;
        this.article = article;
        this.contents = contents;
    }
}
