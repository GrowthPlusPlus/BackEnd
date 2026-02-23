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

    /*
        인증글 신고 목록 조회 (관리자용)
        TODO: @PreAuthorize("hasRole('ROLE_ADMIN')") 추가 예정
     */
    @Operation(
            summary = "신고된 인증글 목록 조회",
            description = """
        신고된 인증글 목록을 신고 횟수 순으로 조회한다.
        ex) GET /api/reports/articles
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고된 인증글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping("/api/reports/articles")
    public ResponseEntity<List<ReportedArticleResponse>> getReportedArticles() {
        List<ReportedArticleResponse> responses = reportService.getReportedArticleList();

        return ResponseEntity.ok(responses);
    }

    /*
        댓글 신고 목록 조회 (관리자용)
        TODO: @PreAuthorize("hasRole('ROLE_ADMIN')") 추가 예정
     */
    @Operation(
            summary = "신고된 댓글 목록 조회",
            description = """
        신고된 댓글 목록을 신고 횟수 순으로 조회한다.
        ex) GET /api/reports/comments
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "신고된 댓글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping("/api/reports/comments")
    public ResponseEntity<List<ReportedCommentResponse>> getReportedComments() {
        List<ReportedCommentResponse> responses = reportService.getReportedCommentList();

        return ResponseEntity.ok(responses);
    }

    /*
        인증글 신고 상세 조회
        TODO: 관리자 전용으로 제한 예정
     */
    @Operation(
            summary = "인증글 신고 상세 조회",
            description = """
        특정 인증글의 신고 상세 내역을 조회한다.
        ex) /api/reports/articles/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증글 신고 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "해당 인증글이 존재하지 않음")
    })
    @GetMapping("/api/reports/articles/{articleId}")
    public ResponseEntity<List<ReportArticleDetailResponse>> getArticleReportDetails(
            @Parameter(description = "인증글 id", required = true)
            @PathVariable Long articleId
    ) {
        List<ReportArticleDetailResponse> responses = reportService.getReportedArticlesDetails(articleId);

        return ResponseEntity.ok(responses);
    }

    /*
        댓글 신고 상세 조회
        TODO: 관리자 전용으로 제한 예정
     */
    @Operation(
            summary = "댓글 신고 상세 조회",
            description = """
        특정 댓글의 신고 상세 내역을 조회한다.
        ex) /api/reports/comments/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "댓글 신고 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "해당 댓글이 존재하지 않음")
    })
    @GetMapping("/api/reports/comments/{commentId}")
    public ResponseEntity<List<ReportCommentDetailResponse>> getCommentReportDetails(
            @Parameter(description = "댓글 id", required = true)
            @PathVariable Long commentId
    ) {
        List<ReportCommentDetailResponse> responses = reportService.getReportedCommentDetails(commentId);

        return ResponseEntity.ok(responses);
    }

    /*
        인증글 신고 처리 (삭제)
        TODO: 관리자 전용으로 제한 예정
     */
    @Operation(
            summary = "인증글 신고 처리",
            description = """
        관리자가 특정 인증글 신고를 삭제(기각 처리)한다.
        ex) DELETE /api/reports/articles/1/2
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "신고 처리 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "신고 내역이 존재하지 않음")
    })
    @DeleteMapping("/api/reports/articles/{articleId}/{reportId}")
    public ResponseEntity<Void> treatArticleReport(
            @Parameter(description = "인증글 id", required = true)
            @PathVariable Long articleId,
            @Parameter(description = "신고내역 id", required = true)
            @PathVariable Long reportId
    ) {
        reportService.treatReportArticle(articleId, reportId);

        return ResponseEntity.noContent().build();
    }

    /*
        댓글 신고 처리 (삭제)
        TODO: 관리자 전용으로 제한 예정
     */
    @Operation(
            summary = "댓글 신고 처리",
            description = """
        관리자가 특정 댓글 신고를 삭제(기각 처리)한다.
        ex) DELETE /api/reports/comments/1/2
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "신고 처리 완료"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "신고 내역이 존재하지 않음")
    })
    @DeleteMapping("/api/reports/comments/{commentId}/{reportId}")
    public ResponseEntity<Void> treatCommentReport(
            @Parameter(description = "댓글 id", required = true)
            @PathVariable Long commentId,
            @Parameter(description = "신고내역 id", required = true)
            @PathVariable Long reportId
    ) {
        reportService.treatReportComment(commentId, reportId);

        return ResponseEntity.noContent().build();
    }
}
