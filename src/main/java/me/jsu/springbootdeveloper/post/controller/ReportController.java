// 정성우

package me.jsu.springbootdeveloper.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.dto.report.*;
import me.jsu.springbootdeveloper.post.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
    인증글/댓글에 대한 신고를 처리하는 클래스이다.
 */
@RequiredArgsConstructor
@RestController
public class ReportController {
    private final ReportService reportService;

    /*
        인증글을 신고하는 컨트롤러이다.
     */
    @Operation(
            summary = "인증글 신고",
            description = """
        특정 인증글을 신고한다.
        한 유저는 같은 인증글을 한 번만 신고할 수 있다.
        ex) POST /api/articles/{articleId}/report
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "신고 성공"),
            @ApiResponse(responseCode = "400", description = "이미 신고한 인증글"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/articles/{articleId}/report")
    public ResponseEntity<Void> reportArticle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "인증글 id", required = true)
            @PathVariable Long articleId,
            @Parameter(description = "신고 내역", required = true)
            @RequestBody ReportRequest request
    ) {
        reportService.reportArticle(userDetails.getUsername(), articleId, request.getReportReason(), request.getDetailReason());

        return ResponseEntity.noContent().build();
    }

    /*
        댓글을 신고하는 컨트롤러이다.
     */
    @Operation(
            summary = "댓글 신고",
            description = """
        특정 댓글을 신고한다.
        한 유저는 같은 댓글을 한 번만 신고할 수 있다.
        ex) POST /api/comments/{commentId}/report
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "신고 성공"),
            @ApiResponse(responseCode = "400", description = "이미 신고한 댓글"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<Void> reportComment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "댓글 id", required = true)
            @PathVariable Long commentId,
            @Parameter(description = "신고 내역", required = true)
            @RequestBody ReportRequest request
    ) {
        reportService.reportComment(userDetails.getUsername(), commentId, request.getReportReason(), request.getDetailReason());

        return ResponseEntity.noContent().build();
    }
}
