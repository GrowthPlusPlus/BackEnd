// 정성우

package me.jsu.springbootdeveloper.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.post.enumType.ReportReason;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/*
    사용자가 신고한 댓글을 관리하기 위한 도메인이다.
 */
@Entity
@Table(
        name = "report_comment",
        uniqueConstraints = {  // 사용자 당 한 번만 댓글을 신고할 수 있다.
                @UniqueConstraint(columnNames = {"user_id", "comment_id"})
        },
        indexes = {
                @Index(
                        name = "idx_comment_id_report_comment",
                        columnList = "comment_id"
                )
        }
)
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)  // 유저가 삭제되도 신고 기록은 남는다.
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reportReason;  // 서버가 미리 지정한 이유이다.

    @Lob
    @Column(nullable = true)
    private String detailReason;  // 사용자가 직접 적은 신고 이유이다.

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public ReportComment(User user, Comment comment, ReportReason reportReason, String detailReason) {
        this.user = user;
        this.comment = comment;
        this.reportReason = reportReason;
        this.detailReason = detailReason;
    }
}