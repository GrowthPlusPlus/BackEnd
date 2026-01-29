// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.Comment;
import me.jsu.springbootdeveloper.post.exception.CommentAccessDeniedException;
import me.jsu.springbootdeveloper.post.exception.CommentNotFoundException;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.dto.CommentResponse;
import me.jsu.springbootdeveloper.post.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/*
    게시글의 댓글을 관리하는 서비스 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class CommentService {
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final PostService postService;

    /*
        댓글을 id로 조회하는 함수이다.
     */
    public Comment findById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("COMMENT_NOT_FOUND_FOR_ID"));
    }

    /*
        인증글의 댓글 목록을 반환한다. 읽기 전용이다.
     */
    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(String email, Long postId, int page) {
        User user = userService.findByEmail(email);

        // 댓글을 한 번에 20개까지만 조회한다.
        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 댓글 개수
                Sort.by("createdAt").descending()  // 생성일을 기준으로 내림차순으로 검색한다.
        );

        // 댓글 정보와 유저 정보를 가져온다.
        Page<Comment> comments = commentRepository.findByArticleIdWithUser(postId, pageable);

        // 댓글을 CommentResponse에 매핑하여 반환한다.
        return comments.map(comment -> CommentResponse.builder()
                .commentId(comment.getId())
                .userNickname(comment.getUser().getNickname())
                // 유저 사진이 없으면 null, 있으면 사진의 주소를 넣는다.
                .userPicture(comment.getUser().getUserProfilePicture() != null ? comment.getUser().getUserProfilePicture().getImageUrl() : null)
                .contents(comment.getContents())  // 댓글 내용이다.
                .createdAt(comment.getCreatedAt())
                .mine(comment.getUser().getId().equals(user.getId()))  // 해당 댓글이 접속한 유저와 같은지 검사한다.
                .build()
        );
    }

    /*
        인증글에 댓글을 작성하는 함수이다.
     */
    @Transactional
    public void createComment(String email, Long postId, String contents) {
        User user = userService.findByEmail(email);
        Article article = postService.findById(postId);  // 댓글을 달려는 게시글을 검색한다.

        Comment comment = Comment.builder()
                .user(user)
                .article(article)
                .contents(contents)
                .build();

        commentRepository.save(comment);  // 작성한 댓글을 저장한다.
        postService.plusCommentNumber(article);  // 댓글 수를 증가시킨다.
    }

    /*
        댓글 id로 댓글 내용을 수정하는 함수이다.
     */
    @Transactional
    public void updateComment(String email, Long commentId, String contents) {
        User user = userService.findByEmail(email);
        Comment comment = findById(commentId);

        authorizeCommentAuthor(comment, user);  // 작성자 검증한다.

        comment.updateContents(contents);  // 댓글 내용을 수정한다.
    }

    /*
        댓글 id로 인증글의 댓글을 삭제하는 함수이다.
     */
    @Transactional
    public void deleteComment(String email, Long commentId) {
        User user = userService.findByEmail(email);
        Comment comment = findById(commentId);

        authorizeCommentAuthor(comment, user);  // 권한을 검사한다.

        Article article = comment.getArticle();  // 댓글 아이디로 게시글을 검색한다.
        postService.minusCommentNumber(article);  // 게시글의 댓글 수를 감소시킨다.

        commentRepository.delete(comment);  // 댓글을 삭제한다.
    }

    /*
        수정/삭제를 하려는 유저가 댓글을 작성한 유저인지 검사하는 함수이다.
     */
    private static void authorizeCommentAuthor(Comment comment, User user) {
        if (!comment.getUser().getId().equals(user.getId())) {  // 유저의 id와 댓글 작성자의 id를 비교한다.
            throw new CommentAccessDeniedException("COMMENT_ACCESS_DENIED");
        }
    }

    /*
        댓글의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachCommentToUser(User user) {
        commentRepository.detachUser(user);
    }
}
