// 정성우

package me.jsu.springbootdeveloper.post.dto.report;

import lombok.Getter;
import me.jsu.springbootdeveloper.post.enumType.ReportReason;

/*
    신고된 인증글의 상세 내용을 담고 있는 dto이다.
 */
@Getter
public class ReportArticleDetailResponse {
    Long reportId;  // 해당 id로 신고를 처리한다.
    Long reporterId;
    String reporterName;
    String challengeName;
    Long articleId;
    String articleContent;
    String imageUrl;  // 인증글의 대표 이미지 하나만 가져온다.
    ReportReason reportReason;
    String detailReason;  // 상세 신고 이유이다.

    public ReportArticleDetailResponse(
            Long reportId,
            Long reporterId,
            String reporterName,
            String challengeName,
            Long articleId,
            String articleContent,
            String imageUrl,
            ReportReason reportReason,
            String detailReason
    ) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.challengeName = challengeName;
        this.articleId = articleId;
        this.articleContent = articleContent;
        this.imageUrl = imageUrl;
        this.reportReason = reportReason;
        this.detailReason = detailReason;
    }
}