// 정성우

package me.jsu.springbootdeveloper.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeDeleteService;
import me.jsu.springbootdeveloper.post.dto.report.ReportArticleDetailResponse;
import me.jsu.springbootdeveloper.post.dto.report.ReportCommentDetailResponse;
import me.jsu.springbootdeveloper.post.dto.report.ReportedArticleResponse;
import me.jsu.springbootdeveloper.post.dto.report.ReportedCommentResponse;
import me.jsu.springbootdeveloper.post.service.CommentService;
import me.jsu.springbootdeveloper.post.service.PostService;
import me.jsu.springbootdeveloper.post.service.ReportService;
import me.jsu.springbootdeveloper.user.dto.ReportedUserResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserWithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    관리자가 사용할 수 있는 컨트롤러들을 모아놓은 클래스이다.
 */
@RestController
@RequiredArgsConstructor
public class AdminController {
    private final ChallengeDeleteService challengeDeleteService;
    private final PostService postService;
    private final CommentService commentService;
    private final UserWithdrawalService userWithdrawalService;
    private final ReportService reportService;
    private final UserService userService;

    /*
        관리자가 방장 여부와 관계없이 챌린지를 삭제한다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "챌린지 삭제 (관리자)",
            description = """
        관리자가 특정 챌린지를 삭제한다. 일반 삭제 API와 달리 방장 검증을 하지 않는다.
        ex) DELETE /api/admin/challenges/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "챌린지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @DeleteMapping("/api/admin/challenges/{challengeId}")
    public ResponseEntity<Void> deleteChallengeAsAdmin(
            @Parameter(description = "챌린지 ID", required = true, example = "10")
            @PathVariable Long challengeId
    ) {
        challengeDeleteService.deleteChallengeAsAdmin(challengeId);

        return ResponseEntity.noContent().build();
    }

    /*
        관리자가 당일 여부·작성자와 관계없이 인증글을 삭제한다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "인증글 삭제 (관리자)",
            description = """
        관리자가 특정 인증글을 삭제한다. 일반 삭제 API와 달리 당일 글 제한·작성자 검증을 하지 않는다.
        ex) DELETE /api/admin/articles/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
    })
    @DeleteMapping("/api/admin/articles/{postId}")
    public ResponseEntity<Void> deleteArticleAsAdmin(
            @Parameter(description = "게시글 ID", required = true, example = "1")
            @PathVariable Long postId
    ) {
        postService.deletePostAsAdmin(postId);

        return ResponseEntity.noContent().build();
    }

    /*
        관리자가 댓글 작성자와 관계없이 댓글을 삭제한다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "댓글 삭제 (관리자)",
            description = """
        관리자가 특정 댓글을 삭제한다. 일반 삭제 API와 달리 작성자 검증을 하지 않는다.
        ex) DELETE /api/admin/comments/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
            @ApiResponse(responseCode = "404", description = "댓글을 찾을 수 없음")
    })
    @DeleteMapping("/api/admin/comments/{commentId}")
    public ResponseEntity<Void> deleteCommentAsAdmin(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId
    ) {
        commentService.deleteCommentAsAdmin(commentId);

        return ResponseEntity.noContent().build();
    }

    /*
        제재당한 유저 목록 조회 (관리자용)
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "제재당한 유저 목록 조회",
            description = """
        제재당한 유저 목록 조회을 제재 횟수 순으로 조회한다.
        ex) GET /api/reported/users
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제재당한 유저 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @GetMapping("/api/reported/users")
    public ResponseEntity<List<ReportedUserResponse>> getReportedUsers() {
        List<ReportedUserResponse> responses = userService.getReportedUsers();

        return ResponseEntity.ok(responses);
    }

    /*
        관리자가 특정 유저를 유예 없이 즉시 삭제한다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "회원 즉시 삭제 (관리자)",
            description = """
        관리자가 userId에 해당하는 계정을 유예 기간 없이 물리 삭제한다.
        방장 챌린지는 일반 탈퇴와 동일하게 위임·삭제 처리된다. 관리자 본인 계정은 삭제할 수 없다.
        ex) DELETE /api/admin/users/1
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "관리자 권한 없음 또는 본인 삭제 시도"),
            @ApiResponse(responseCode = "404", description = "유저를 찾을 수 없음")
    })
    @DeleteMapping("/api/admin/users/{userId}")
    public ResponseEntity<Void> adminHardDeleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "삭제할 유저 id", required = true, example = "1")
            @PathVariable Long userId
    ) {
        userWithdrawalService.adminHardDeleteUser(userId, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }

    /*
        인증글 신고 목록 조회 (관리자용)
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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
