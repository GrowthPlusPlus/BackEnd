// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.Comment;
import me.jsu.springbootdeveloper.post.exception.CommentAccessDeniedException;
import me.jsu.springbootdeveloper.post.exception.CommentNotFoundException;
import me.jsu.springbootdeveloper.post.exception.NotFoundException;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.dto.comment.CommentResponse;
import me.jsu.springbootdeveloper.post.repository.CommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/*
    게시글의 댓글을 관리하는 서비스 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class CommentService {
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final PostService postService;
    private final PostQueryService postQueryService;
    private final FcmService fcmService;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final UserProfilePictureRepository userProfilePictureRepository;

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
                .userId(comment.getUser().getId())
                .userNickname(comment.getUser().getNickname())
                // 유저 사진이 없으면 null, 있으면 사진의 주소를 넣는다.
                .userPicture(comment.getUser().getUserProfilePicture() != null ? comment.getUser().getUserProfilePicture().getImageUrl() : null)
                .contents(comment.getContents())  // 댓글 내용이다.
                .updatedAt(comment.getUpdateAt())
                .createdAt(comment.getCreatedAt())
                .isEdited(Boolean.TRUE.equals(comment.getIsEdited()))
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
        Article article = postQueryService.findById(postId);  // 댓글을 달려는 게시글을 검색한다.

        // 댓글을 작성한 정확한 시간이다.
        LocalDateTime postDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        Comment comment = Comment.builder()
                .user(user)
                .article(article)
                .contents(contents)
                .updateAt(postDateTime)
                .build();

        commentRepository.save(comment);  // 작성한 댓글을 저장한다.
        postService.plusCommentNumber(article);  // 댓글 수를 증가시킨다.

        //게시글 작성자에게 인앱&푸시 알림 발송
        //홍길동님이 내 '챌린지명''0일차' 에 댓글을 달았어요: "댓글 내용"
        User writer = postRepository.findUserByArticleId(postId)
                .orElseThrow(() -> new NotFoundException("작성자를 찾을 수 없습니다."));
        Challenge challenge = postRepository.findChallengeByArticleId(postId)
                .orElseThrow(() -> new NotFoundException("연관된 챌린지를 찾을 수 없습니다."));
        long daysPassed = challenge.getChallengeDayCount(); // "n일차"를 계산한다.
        String title = "새 댓글";
        String message = String.format("%s님이 내 '%s' %d일차에 댓글을 달았어요: \"%s\"",
                user.getNickname(),
                challenge.getTitle(),
                daysPassed,
                comment.getContents());
        //인앱 알림 발송
        //댓글 작성자의 프로필 이미지도 notificaiton객체로 같이 저장한다, targetId: 게시글 Id
        long commentUserId = user.getId();
        String imageUrl = userProfilePictureRepository.findImageUrlByUserId(commentUserId).orElse("");
        notificationService.sendNotification(writer, NotificationType.COMMENT, message, imageUrl, postId);

        // 푸시 알림을 발송
        // 챌린지 푸시 알림이 활성화되어 있고, FCM 토큰이 있는 경우에만 발송한다.
        if (writer.isAllPushNotificationEnabled()
                && writer.getFcmToken() != null
                && !writer.getFcmToken().isBlank()) {
            fcmService.sendMessageTo(writer.getFcmToken(), title, message);

        }
    }

    /*
        댓글 id로 댓글 내용을 수정하는 함수이다.
     */
    @Transactional
    public void updateComment(String email, Long commentId, String contents) {
        User user = userService.findByEmail(email);
        Comment comment = findById(commentId);

        authorizeCommentAuthor(comment, user);  // 작성자를 검증한다.

        LocalDateTime postDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        comment.updateContents(contents, postDateTime);  // 댓글 내용을 수정한다.
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
    public void detachCommentToUser(List<Long> userIds) {
        commentRepository.detachUser(userIds);
    }

    /*
        관리자가 댓글 작성자와 관계없이 댓글을 삭제할 때 사용한다.
        @param commentId 삭제할 댓글 id
     */
    @Transactional
    public void deleteCommentAsAdmin(Long commentId) {
        Comment comment = findById(commentId);
        Article article = comment.getArticle();  // 댓글 아이디로 게시글을 검색한다.

        // 인증글의 총 댓글 수를 감소시킨다.
        postService.minusCommentNumber(article);

        // 댓글을 삭제한다.
        commentRepository.delete(comment);
    }
}
