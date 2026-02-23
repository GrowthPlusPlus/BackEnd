// 정성우

package me.jsu.springbootdeveloper.post.dto.report;

import lombok.Getter;
import me.jsu.springbootdeveloper.post.enumType.ReportReason;

/*
    인증글/댓글을 신고하기 위해 사용하는 dto이다.
 */
@Getter
public class ReportRequest {
    private ReportReason reportReason;
    private String detailReason;
}
