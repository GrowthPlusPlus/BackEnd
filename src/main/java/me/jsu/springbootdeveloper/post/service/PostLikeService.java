// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.PostLike;
import me.jsu.springbootdeveloper.post.exception.ArticleNotFoundException;
import me.jsu.springbootdeveloper.post.exception.DuplicatePostLikeException;
import me.jsu.springbootdeveloper.post.exception.PostLikeNotFoundException;
import me.jsu.springbootdeveloper.post.exception.NotFoundException;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.post.repository.PostLikeRepository;
import org.springframework.stereotype.Service;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;

import java.util.List;

/*
    인증글의 좋아요를 처리하는 서비스 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final UserService userService;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final ChallengeQueryService challengeQueryService;
    private final FcmService fcmService;
    private final UserProfilePictureRepository userProfilePictureRepository;

    /*
        게시글 좋아요의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachLikeToUser(List<Long> userIds) {
        postLikeRepository.detachUser(userIds);
    }

    /*
        해당 유저가 게시글에 좋아요를 눌렀는지 반환해주는 함수이다.
     */
    public boolean existsByArticle_IdAndUser_IdForLike(Long postId, Long userId) {
        return postLikeRepository.existsByArticle_IdAndUser_Id(postId, userId);
    }

    /*
        해당 유저가 여러 게시글에 대해 좋아요를 눌렀는지 반환해주는 함수이다.
     */
    public List<Long> findLikedPostIds(Long userId, List<Long> postIds) {
        return postLikeRepository.findLikedPostIds(userId, postIds);
    }

    /*
        유저가 게시글에 좋아요를 눌렀을때 처리하는 함수이다.
     */
    @Transactional
    public void likePost(String email, Long postId) {
        User user = userService.findByEmail(email);
        Article article = postRepository.findById(postId)  // 순환 참조를 방지하기 위해 직접 postService를 사용하지 않는다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        // 같은 게시글에 좋아요를 두 번 누르는 경우 예외처리 한다.
        if (existsByArticle_IdAndUser_IdForLike(postId, user.getId())) {
            throw new DuplicatePostLikeException("CANNOT_LIKE_TWICE");
        }

        PostLike postLike = PostLike.builder()
                .user(user)
                .article(article)
                .build();

        postLikeRepository.save(postLike);

        article.plusLikeNumber();

        //좋아요 눌렀을때 푸시 알림 전송, 전체 알림 활성화 되어있으면 발송
        //게시글 작성자(writer)에게 알림 보내기
        //홍길동님이 내 '챌린지명''0일차' 인증글을 좋아해요.
        User writer = postRepository.findUserByArticleId(postId)
                .orElseThrow(() -> new NotFoundException("해당 게시글의 작성자를 찾을 수 없습니다.")); // Optional을 안전하게 벗겨낸다.
        long userId = user.getId();
        //좋아요 누른 사람이 나 자신이 아닐 경우에만 인앱&푸시 알림 발송
        if (userId != writer.getId()) {
            Challenge challenge = postRepository.findChallengeByArticleId(postId)
                    .orElseThrow(() -> new NotFoundException("연관된 챌린지를 찾을 수 없습니다.")); // Optional에서 객체를 꺼낸다.
            long daysPassed = challenge.getChallengeDayCount(); // "n일차"를 계산한다.
            String title = "게시글 좋아요";
            String message = String.format("%s님이 내 '%s' %d일차 인증글을 좋아해요.",
                    user.getNickname(),
                    challenge.getTitle(),
                    daysPassed
            );

            // 푸시 알림&인앱 알림 발송 - 작성자에게 발송
            // 좋아요 누른 사람의 profileurl notification객체에 함께 저장
            String imageUrl = userProfilePictureRepository.findImageUrlByUserId(userId).orElse("");
            notificationService.sendNotification(writer, NotificationType.LIKE, message, imageUrl, postId);

            // 챌린지별 멤버 반응 소식 설정이 활성화되어 있고, 진행중이며, FCM 토큰이 있을 때에만 발송한다.
            ChallengeParticipant writerParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(writer.getId(), challenge.getId());
            boolean inProgress = writerParticipant.getStatus() == Status.IN_PROGRESS;
            boolean challengeAllEnabled = writerParticipant.isChallengeAllPushEnabled();
            boolean reactionEnabled = writerParticipant.isLikesPushEnabled();
            boolean hasFcm = writer.getFcmToken() != null && !writer.getFcmToken().isBlank();
            if (inProgress && challengeAllEnabled && reactionEnabled && hasFcm) {
                fcmService.sendMessageTo(writer.getFcmToken(), title, message);
            }
        }
    }

    /*
        좋아요를 삭제하는 함수이다.
     */
    @Transactional
    public void unlikePost(String email, Long postId) {
        User user = userService.findByEmail(email);
        Article article = postRepository.findById(postId)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        PostLike postLike = postLikeRepository.findByArticle_IdAndUser_Id(article.getId(), user.getId())
                .orElseThrow(() -> new PostLikeNotFoundException("POST_LIKE_NOT_FOUND"));

        postLikeRepository.delete(postLike);

        article.minusLikeNumber();
    }
}
