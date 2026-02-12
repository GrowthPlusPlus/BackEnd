// 정성우

package me.jsu.springbootdeveloper.post.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.post.enumType.ReportReason;

/*
    신고된 댓글의 상세 내용을 담고 있는 dto이다.
 */
@Getter
public class ReportCommentDetailResponse {
    Long reportId;          // 신고 객체 id
    Long reporterId;        // 신고자 id
    String reporterName;    // 신고자 이름
    Long commentId;         // 댓글 id
    String commentContent;  // 댓글 내용
    Long articleId;         // 댓글이 속한 게시글 id
    String challengeName;
    ReportReason reportReason;
    String detailReason;

    public ReportCommentDetailResponse(
            Long reportId,
            Long reporterId,
            String reporterName,
            Long commentId,
            String commentContent,
            Long articleId,
            String challengeName,
            ReportReason reportReason,
            String detailReason
    ) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.commentId = commentId;
        this.commentContent = commentContent;
        this.articleId = articleId;
        this.challengeName = challengeName;
        this.reportReason = reportReason;
        this.detailReason = detailReason;
    }
}
