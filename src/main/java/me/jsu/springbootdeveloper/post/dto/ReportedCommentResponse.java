// 정성우

package me.jsu.springbootdeveloper.post.dto;

import lombok.Getter;

@Getter
public class ReportedCommentResponse {
    Long commentId;
    String challengeName;   // nullable 가능
    int totalReport;

    public ReportedCommentResponse(
            Long commentId,
            String challengeName,
            int totalReport
    ) {
        this.commentId = commentId;
        this.challengeName = challengeName;
        this.totalReport = totalReport;
    }
}
