// 정성우

package me.jsu.springbootdeveloper.post.dto;

import lombok.Getter;

/*
    신고된 인증글에 대한 대략적인 정보를 가진 dto이다.
 */
@Getter
public class ReportedArticleResponse {
    Long articleId;
    String challengeName;
    int totalReport;

    public ReportedArticleResponse(
            Long articleId,
            String challengeName,
            int totalReport
    ) {
        this.articleId = articleId;
        this.challengeName = challengeName;
        this.totalReport = totalReport;
    }
}
