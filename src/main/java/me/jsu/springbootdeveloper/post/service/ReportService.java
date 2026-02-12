// 정성우

package me.jsu.springbootdeveloper.post.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.Comment;
import me.jsu.springbootdeveloper.post.domain.ReportArticle;
import me.jsu.springbootdeveloper.post.domain.ReportComment;
import me.jsu.springbootdeveloper.post.dto.*;
import me.jsu.springbootdeveloper.post.enumType.ReportReason;
import me.jsu.springbootdeveloper.post.exception.DuplicateReportException;
import me.jsu.springbootdeveloper.post.exception.InvalidReportTargetException;
import me.jsu.springbootdeveloper.post.exception.ReportNotFoundException;
import me.jsu.springbootdeveloper.post.repository.CommentRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.post.repository.ReportArticleRepository;
import me.jsu.springbootdeveloper.post.repository.ReportCommentRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
    인증글/댓글의 신고를 처리하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {
    private final ReportArticleRepository reportArticleRepository;
    private final ReportCommentRepository reportCommentRepository;
    private final PostService postService;
    private final PostRepository postRepository;
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /*
        인증글을 신고하는 함수이다.
     */
    public void reportArticle(String email, Long articleId, ReportReason reportReason, String detailReason) {
        User user = userService.findByEmail(email);
        Article article = postService.findById(articleId);

        // 유저는 한 인증글 당 한 번만 신고가 가능하다.
        if (reportArticleRepository.existsByUser_IdAndArticle_Id(user.getId(), articleId)) {
            throw new DuplicateReportException("이미 신고한 인증글입니다.");
        }

        // 신고를 생성한다.
        reportArticleRepository.save(ReportArticle.builder()
                .user(user)
                .article(article)
                .reportReason(reportReason)
                .detailReason(detailReason)
                .build());

        // 신고 수를 증가시킨다.
        article.plusTotalReport();
    }

    /*
        댓글을 신고하는 함수이다.
     */
    public void reportComment(String email, Long commentId, ReportReason reportReason, String detailReason) {
        User user = userService.findByEmail(email);
        Comment comment = commentService.findById(commentId);

        // 유저는 한 댓글 당 한 번만 신고가 가능하다.
        if (reportCommentRepository.existsByUser_IdAndComment_Id(user.getId(), commentId)) {
            throw new DuplicateReportException("이미 신고한 댓글입니다.");
        }

        // 신고를 생성한다.
        reportCommentRepository.save(ReportComment.builder()
                .user(user)
                .comment(comment)
                .reportReason(reportReason)
                .detailReason(detailReason)
                .build());

        // 신고 수를 증가시킨다.
        comment.plusTotalReport();
    }

    /*
        모든 인증글 신고 내역을 대략적으로 조회하는 함수이다.
     */
    @Transactional(readOnly = true)
    public List<ReportedArticleResponse> getReportedArticleList() {
        return postRepository.findReportedArticles();
    }

    /*
        모든 인증글 신고 내역을 대략적으로 조회하는 함수이다.
     */
    @Transactional(readOnly = true)
    public List<ReportedCommentResponse> getReportedCommentList() {
        return commentRepository.findReportedComments();
    }

    /*
        한 인증글의 모든 신고 내역을 조회하는 함수이다.
     */
    @Transactional(readOnly = true)
    public List<ReportArticleDetailResponse> getReportedArticlesDetails(Long articleId) {
        return reportArticleRepository.findReportArticleDetails(articleId);
    }

    /*
        한 댓글의 모든 신고 내역을 조회하는 함수이다.
     */
    @Transactional(readOnly = true)
    public List<ReportCommentDetailResponse> getReportedCommentDetails(Long commentId) {
        return reportCommentRepository.findReportCommentDetails(commentId);
    }

    /*
        관리자가 신고 내역을 보고 부당하다고 생각하는 경우, 인증글 신고를 삭제하는 함수이다.
     */
    public void treatReportArticle(Long articleId, Long reportArticleId) {
        ReportArticle reportArticle = reportArticleRepository.findById(reportArticleId)
                .orElseThrow(() -> new ReportNotFoundException("삭제할 인증글 신고가 없습니다."));

        // 매게변수로 들어온 인증글 id와 신고내역의 인증글 id가 같은지 확인한다.
        if (!reportArticle.getArticle().getId().equals(articleId)) {
            throw new InvalidReportTargetException("인증글 id와 신고내역의 인증글 id가 서로 다릅니다.");
        }

        reportArticleRepository.delete(reportArticle);  // db에서 삭제한다.

        // 인증글을 검색하여 신고 수를 감소시킨다.
        Article article = postService.findById(articleId);
        article.minusTotalReport();
    }

    /*
        관리자가 신고 내역을 보고 부당하다고 생각하는 경우, 댓글 신고를 삭제하는 함수이다.
     */
    public void treatReportComment(Long commentId, Long reportCommentId) {
        ReportComment reportComment = reportCommentRepository.findById(reportCommentId)
                .orElseThrow(() -> new ReportNotFoundException("삭제할 댓글 신고가 없습니다."));

        if (!reportComment.getComment().getId().equals(commentId)) {
            throw new InvalidReportTargetException("댓글 id와 신고내역의 댓글 id가 서로 다릅니다.");
        }

        reportCommentRepository.delete(reportComment);  // db에서 삭제한다.

        // 댓글을 검색하여 신고 수를 감소시킨다.
        Comment comment = commentService.findById(commentId);
        comment.minusTotalReport();
    }

    /*
        신고 인증글의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    public void detachReportArticleToUser(Long userId) {
        reportArticleRepository.detachUser(userId);
    }

    /*
        신고 댓글의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    public void detachReportCommentToUser(Long userId) {
        reportCommentRepository.detachUser(userId);
    }
}