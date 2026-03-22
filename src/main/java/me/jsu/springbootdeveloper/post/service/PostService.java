// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.service.ChallengeDeleteService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.post.dto.article.ArticleImageResponse;
import me.jsu.springbootdeveloper.post.event.CloudinaryDeleteEvent;
import me.jsu.springbootdeveloper.post.exception.*;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.post.domain.PostPicture;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.post.dto.article.ArticleDetailResponse;
import me.jsu.springbootdeveloper.post.repository.PostPictureRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final ChallengeService challengeService;
    private final PostLikeService postLikeService;
    private final PostQueryService postQueryService;
    private final PostPictureRepository postPictureRepository;
    private final UserStatisticsService userStatisticsService;
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeDeleteService challengeDeleteService;
    private final ImageTemporaryService imageTemporaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final FcmService fcmService;
    private static final int MAX_IMAGE_COUNT = 3;

    /*
        게시글 상제 정보를 게시글 id로 조회하는 함수이다.
     */
    public ArticleDetailResponse getPost(String email, Long postId) {
        User user = userService.findByEmail(email);

        Article article = postRepository.findWithUserAndProfileById(postId)  // 인증글과 작성자 정보, 사진을 함께 조회한다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        // 챌린지나 유저가 삭제되었을 때를 위한 방어 코드이다.
        User author = article.getUser();
        Challenge challenge = article.getChallenge();

        Long userId = null;
        String userNickname;
        String userImageUrl = null;  // 작성자의 프로필 사진이 없는 경우를 대비한다.
        int totalSuccessDays = 0;
        Long challengeId = null;
        String challengeTitle = null;

        if (author == null) {  // 작성자가 null인 경우를 방어한다.
            userNickname = "탈퇴한 사용자";
        } else {
            userId = author.getId();
            userNickname = author.getNickname();

            UserProfilePicture userProfilePicture = author.getUserProfilePicture();
            if (userProfilePicture != null) {  // 프로필 사진이 존재하는지 검색한다.
                userImageUrl = userProfilePicture.getImageUrl();
            }

            // participant 조회는 challenge가 존재할 때만 한다.
            if (challenge != null) {
                ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(author.getId(), challenge.getId());

                if (challengeParticipant != null) {
                    totalSuccessDays = challengeParticipant.getTotalSuccessDays();  // 해당 챌린지의 총 인증 횟수를 구한다.
                }
            }
        }

        // 현재 조회한 유저가 게시글 작성자인지 확인한다.
        boolean isAuthor = author != null && author.getId().equals(user.getId());

        // 챌린지가 null인 경우를 방어한다.
        if (challenge == null) {
            challengeTitle = "삭제된 챌린지";
        }
        else {
            challengeId = challenge.getId();
            challengeTitle = challenge.getTitle();
        }

        // 인증글의 사진, id 리스트를 생성한다.
        List<PostPicture> postPictures = postPictureRepository.findAllByArticle_Id(article.getId());
        List<ArticleImageResponse> images = postPictures.stream()
                .map(pp -> ArticleImageResponse.builder()
                        .imageId(pp.getId())
                        .imageUrl(pp.getImageUrl())
                        .build())
                .toList();

        // 게시글을 조회한 유저가 해당 게시글에 좋아요를 눌렀는지에 대한 여부를 검색한다.
        boolean liked = postLikeService.existsByArticle_IdAndUser_IdForLike(postId, user.getId());

        return ArticleDetailResponse.builder()
                .postId(article.getId())
                .challengeId(challengeId)
                .challengeTitle(challengeTitle)
                .totalSuccessDays(totalSuccessDays)  // 총 성공 일수이다.
                .title(article.getTitle())
                .content(article.getContent())
                .images(images)  // 게시글의 사진, id이다.
                .userId(userId)
                .userNickname(userNickname)
                .userImageUrl(userImageUrl)  // 게시글 작성자의 사진이다.
                .updatedAt(article.getUpdateAt())
                .createdAt(article.getCreatedAt())
                .isEdited(Boolean.TRUE.equals(article.getIsEdited()))
                .isAuthor(isAuthor)
                .likeNumber(article.getLikeNumber())  // 좋아요 개수이다.
                .commentNumber(article.getCommentNumber())  // 댓글 개수이다.
                .liked(liked)
                .build();
    }

    /*
        게시글을 추가하는 함수이다.
        유저로부터 받은 이미지는 검증 서비스를 거치고 왔다고 가정하고 수행한다.
     */
    @Transactional
    public ArticleDetailResponse save(String email, String content, Long challengeId, List<Long> tempImageIds) {
        // 유저 이메일로 user 객체를 검색한다.
        User user = userService.findByEmail(email);

        // request로 온 챌린지 id로 post가 어떤 챌린지에 속하는지 검색한다.
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 현재 진행중인 챌린지가 아니라면 예외를 던진다.
        ChallengeParticipant challengeParticipant = challengeQueryService.challengeStatusInProgress(user.getId(), challengeId);

        // 챌린지 인증 시각을 정확하게 보기 위해 생성 시각과는 다른 인증 날짜 도메인을 생성한다.
        LocalDateTime postDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 인증날짜용 도메인이다.
        LocalDate postDate = postDateTime.toLocalDate();

        // 하루 1회로 인증을 제한한다.
        boolean alreadyPosted = postRepository.existsByUser_IdAndChallenge_IdAndPostDate(user.getId(), challenge.getId(), postDate);
        if (alreadyPosted) {
            throw new DuplicateDailyPostException("DAILY_POST_ALREADY_EXISTS");
        }

        // 이미지가 아에 비어있으면 빈 리스트를 대입한다.
        tempImageIds = (tempImageIds == null) ? List.of() : tempImageIds;

        // 챌린지의 이미지 필수 여부를 확인한다.
        boolean photoRequired = challenge.isPhotoRequired();
        if (photoRequired && tempImageIds.isEmpty()) {
            throw new InvalidImageException("IMAGE_REQUIRED");  // 사진이 필수 인증인데 이미지가 없다면 예외처리 한다.
        }

        // 업로드 한 사진의 개수가 5개보다 많으면 예외처리 한다.
        if (tempImageIds.size() > MAX_IMAGE_COUNT) {
            throw new InvalidImageException("MAX_IMAGE_COUNT_EXCEEDED");
        }

        // 서버에 글을 저장한다.
        Article article = postRepository.save(Article.builder()
                .content(content)
                .user(user)
                .challenge(challenge)
                .updateAt(postDateTime)
                .postDate(postDate)
                .build());

        // 검증된 사진을 DB와 클라우디너리에 저장한다.
        saveImages(tempImageIds, article, user.getId(), challengeId);

        // 인증글을 업데이트 했으므로 총 인증 횟수와 연속 인증 횟수를 업데이트 한다.
        challengeService.succeedChallenge(challengeParticipant, postDate);

        // 인증글의 제목을 업데이트 한다.
        article.updateTitle(challenge.getTitle() + " " + challengeParticipant.getTotalSuccessDays() + "일차");

        // 인증글을 업데이트 했으므로 챌린지 데일리 로그도 업데이트 한다.
        challengeService.createDailyLog(user, challenge, article, postDate);

        // 오늘 인증한 사람 수를 증가시킨다.
        challengeService.updateTodaySuccessCount(challenge, postDate);

        // 주의 시작인 월요일을 계산한다.
        LocalDate weekStartDate = postDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 인증글을 업데이트 했으므로 챌린지 주간 상태도 업데이트 한다.
        challengeService.updateWeeklyStatus(user, challenge, weekStartDate, challenge.getRequiredWeeklyCount());

        // 유저 통계를 업데이트 한다.
        userStatisticsService.onPostConfirmed(user, challenge.getId(), postDate);

        /*
            챌린지 멤버 인증 알림 전송 - 삭제
            challenge_participant중 상태가 in progress 인 사람에게 전송
            "홍길동 님이 [챌린지명]챌린지에 인증했어요!"

        List<ChallengeParticipant> challengeParticipants = challengeParticipantRepository.findParticipantsInProgress(challenge.getId(), user.getId(), Status.IN_PROGRESS);
        for (ChallengeParticipant Participant : challengeParticipants) {
            User participatingUser = Participant.getUser();
            //인앱알림 보내기, 게시글 작성자 프로필이미지 notification객체에 함께 저장
            long userId = user.getId();
            String imageUrl = userProfilePictureRepository.findImageUrlByUserId(userId).orElse("");
            notificationService.sendNotification(participatingUser, NotificationType.CHALLENGE, participatingUser.getNickname() + "님이 " + challenge.getTitle() + "챌린지에 인증했어요!", imageUrl);
            //전체 알림 활성화 검사하고 푸시 알림 보내기
            if (participatingUser.isAllPushNotificationEnabled()) {
                fcmService.sendMessageTo(participatingUser.getFcmToken(), "챌린지 인증", participatingUser.getNickname() + "님이 " + challenge.getTitle() + "챌린지에 인증했어요!");
            }


        }
        */
        // 저장한 인증글 id로 검색하여 인증글 상세 정보를 반환한다.
        return getPost(email, article.getId());
    }

    /*
        클라우디너리에 사진을 저장하고, 사진 정보를 DB에 반영해주는 함수이다.
     */
    public void saveImages(List<Long> tempImageIds, Article article, Long userId, Long challengeId) {
        // 임시 파일들을 검색한다.
        List<PostPictureTemporary> postPictureTemporaries = imageTemporaryService.findAllById(tempImageIds);

        // 검색한 임시 파일들의 개수와 검색에 사용한 id 개수가 같은지 비교한다.
        if (postPictureTemporaries.size() != tempImageIds.size()) {
            throw new InvalidImageException("검색된 임시파일 개수와 검색에 사용한 id 개수가 다릅니다.");
        }

        // 업로드하려는 임시 사진 파일이 해당 유저가 업로드 한 것인지, 다른 챌린지에 업로드한 사진이 아닌지 검사한다.
        for (PostPictureTemporary postPictureTemporary : postPictureTemporaries) {
            if (!postPictureTemporary.getUser().getId().equals(userId)) {
                throw new InvalidImageException("임시로 업로드 한 유저와 사용하려는 유저가 다릅니다.");
            }
            if (!postPictureTemporary.getChallenge().getId().equals(challengeId)) {
                throw new InvalidImageException("임시로 업로드 했을 때의 챌린지 id와 현재 인증하려는 챌린지 id가 다릅니다.");
            }

            // 임시 사진 파일의 정보를 이용하여 실제 인증글 사진 DB에 저장한다.
            postPictureRepository.save(PostPicture.builder()
                    .article(article)
                    .cloudinaryPublicId(postPictureTemporary.getCloudinaryPublicId())
                    .imageUrl(postPictureTemporary.getImageUrl())
                    .build());

            // 사용이 끝난 임시파일의 DB 정보를 삭제한다.
            imageTemporaryService.deleteTempFile(postPictureTemporary);
        }
    }

    /*
        클라우디너리의 사진을 삭제하고, 사진 정보를 DB에서 삭제하는 함수이다.
     */
    public void deleteImages(List<PostPicture> postPictures) {
        // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다. 이미지가 없는 경우엔 if문을 실행하지 않는다.
        if (!postPictures.isEmpty()) {
            for (PostPicture pp : postPictures) {
                String publicId = pp.getCloudinaryPublicId();

                // 우리 서버의 DB에서 정보를 삭제한다.
                postPictureRepository.delete(pp);

                // 클라우디너리의 이미지를 삭제한다. 모든 트렌젝션이 끝난 후에 수행한다.
                applicationEventPublisher.publishEvent(new CloudinaryDeleteEvent(publicId));
            }
        }
    }

    /*
        게시글 하나를 삭제하는 함수로, 게시글 작성자 id가 요청자 id와 다르면 삭제하지 않는다.
        또한 당일 인증글만 삭제 가능하다.
     */
    @Transactional
    public void delete(String email, Long postId) {
        User user = userService.findByEmail(email);
        Article article = postQueryService.findWithChallengeById(postId);  // 삭제할 글을 찾는다.

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (!today.isEqual(article.getPostDate())) {  // 과거의 글을 삭제하려는 경우이다.
            throw new ArticleAccessDeniedException("PAST_POST_CANNOT_DELETE");
        }

        // 글에 있는 사진을 찾는다.
        List<PostPicture> postPictures = postPictureRepository.findAllByArticle_Id(postId);

        authorizeArticleAuthor(article, user);  // 글 작성자인지 확인한다.

        // 챌린지 데일리 로그 정보는 게시글이 삭제되면 삭제한다.
        ChallengeDailyLog challengeDailyLog = challengeQueryService.findLogByUser_IdAndArticle_IdAndPostDate(user.getId(), postId, article.getPostDate());
        challengeDeleteService.deleteLog(challengeDailyLog);

        deleteImages(postPictures);  // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다.

        // 챌린지의 오늘 성공 횟수를 감소시킨다.
        Challenge challenge = article.getChallenge();
        challenge.decreaseTodaySuccess();

        // 해당 유저의 총 챌린지 성공 일수를 감소시킨다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challenge.getId());
        challengeParticipant.removeDailySuccess(article.getPostDate());

        // 이번주 챌린지 총 성공 횟수를 감소시킨다.
        LocalDate thisWeekMonday = article.getPostDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ChallengeWeeklyStatus challengeWeeklyStatus = challengeQueryService.findWeeklyStatus(user.getId(), challenge.getId(), thisWeekMonday);
        challengeWeeklyStatus.decreaseSuccessCount(challenge.getRequiredWeeklyCount());

        // 유저 통계의 내용을 롤백한다.
        userStatisticsService.onTodayPostCancelled(user.getId(), challenge.getId(), article.getPostDate());

        postRepository.delete(article);  // DB에서 게시글을 삭제한다. 댓글이나 좋아요 등의 정보도 함께 삭제된다.
    }

    /*
        인증글을 수정하는 함수이다.
        수정하는 값은 인증 내용, 사진이다.
     */
    @Transactional
    public ArticleDetailResponse update(String email, Long postId, String content, List<Long> deleteImageIds, List<Long> tempImageIds) {
        User user = userService.findByEmail(email);
        Article article = postQueryService.findById(postId);

        authorizeArticleAuthor(article, user);  // 글 작성자인지 확인한다.

        // 이미지가 비어있는 경우 빈 리스트로 정규화 한다.
        tempImageIds = (tempImageIds == null) ? List.of() : tempImageIds;

        // 삭제하려는 이미지가 비어있는 경우 빈 리스트로 정규화 한다.
        deleteImageIds = (deleteImageIds == null) ? List.of() : deleteImageIds;

        // 기존 사진 객체를 검색한다.
        List<PostPicture> postPictures = postPictureRepository.findAllById(deleteImageIds);

        // 삭제를 요청한 이미지 개수와 서버에 존재하는 이미지 개수가 일치하는지 확인한다.
        if (postPictures.size() != deleteImageIds.size()) {
            throw new InvalidImageException("IMAGE_NOT_FOUND");
        }

        // 삭제하려는 이미지가 현재 수정 중인 게시글의 이미지인지 확인한다.
        for (PostPicture pp : postPictures) {
            if (!pp.getArticle().getId().equals(article.getId())) {
                throw new InvalidImageException("INVALID_IMAGE_ACCESS");
            }
        }

        int currentCount = (int) postPictureRepository.countByArticle_Id(article.getId());  // 기존 사진 개수를 검색한다.
        int deleteCount = deleteImageIds.size();  // 삭제할 사진 개수
        int newCount = tempImageIds.size();  // 새로 추가할 사진 개수

        int finalCount = currentCount - deleteCount + newCount;  // 최종 사진 개수

        if (finalCount > MAX_IMAGE_COUNT) {
            throw new InvalidImageException("MAX_IMAGE_COUNT_EXCEEDED");
        }

        // 업데이트 되는 시간이다.
        LocalDateTime postDateTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 업데이트 하는 시간이 업로드한 날에서 자정이 지난 시점이라면 예외처리 한다. 즉 12시가 지나면 바로 수정이 불가능하다.
        if (!article.getPostDate().equals(postDateTime.toLocalDate())) {
            throw new PostModificationPeriodExpiredException("하루가 지나면 수정할 수 없습니다.");
        }

        // 인증글 내용을 업데이트 한다.
        article.updateContent(content, postDateTime);

        // 인증글의 기존 이미지를 삭제한다.
        deleteImages(postPictures);

        // 인증글의 사진을 업데이트 한다.
        saveImages(tempImageIds, article, user.getId(), article.getChallenge().getId());

        // 수정된 게시글 정보를 반환한다.
        return getPost(email, postId);
    }

    /*
        인증글을 작성한 유저와 요청한 유저가 동일 인물인지 검증한다.
     */
    private static void authorizeArticleAuthor(Article article, User user) {
        if (!article.getUser().getId().equals(user.getId())) {  // 유저의 id와 인증글 작성자의 id를 비교한다.
            throw new ArticleAccessDeniedException("ARTICLE_ACCESS_DENIED");
        }
    }

    /*
        게시글의 댓글 수를 증가시키는 함수이다.
     */
    @Transactional
    public void plusCommentNumber(Article article) {
        article.plusCommentNumber();
    }

    /*
        게시글의 댓글 수를 감소시키는 함수이다.
     */
    @Transactional
    public void minusCommentNumber(Article article) {
        article.minusCommentNumber();
    }

    /*
        게시글의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachPostToUser(List<Long> userIds) {
        postRepository.detachUser(userIds);
    }

    /*
        관리자가 인증글을 삭제한다.
        서울 기준 당일 글이면 일반 삭제와 동일한 롤백(decreaseTodaySuccess, onTodayPostCancelled)을 적용한다.
        과거 글이면 챌린지·주간·참가자 롤백은 유지하되, 오늘 성공 카운트와 당일 전용 UserStatistics API는 쓰지 않고 삭제 후 남은 ChallengeDailyLog로 유저 전역 통계를 재동기화한다.

        @param postId 삭제할 인증글 id
     */
    @Transactional
    public void deletePostAsAdmin(Long postId) {
        Article article = postQueryService.findWithChallengeById(postId);
        Long authorId = article.getUser().getId();  // 인증글 작성자의 id를 저장한다.

        LocalDate postDate = article.getPostDate();  // 인증글의 생성 날짜를 검색한다.
        LocalDate todaySeoul = LocalDate.now(ZoneId.of("Asia/Seoul"));  // 삭제 당일 시간을 검색한다.
        boolean isTodayPost = todaySeoul.isEqual(postDate);  // 두 날짜가 같은지 확인한다.

        // 인증글의 사진 정보를 검색한다.
        List<PostPicture> postPictures = postPictureRepository.findAllByArticle_Id(postId);

        // 챌린지 데일리 로그 정보는 게시글이 삭제되면 삭제한다.
        ChallengeDailyLog challengeDailyLog = challengeQueryService.findLogByUser_IdAndArticle_IdAndPostDate(authorId, postId, postDate);
        challengeDeleteService.deleteLog(challengeDailyLog);

        deleteImages(postPictures);  // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다.

        Challenge challenge = article.getChallenge();  // 인증글의 챌린지 정보를 가져온다.

        // 당일 글일 때만 챌린지 "오늘 성공" 카운터를 줄인다.
        if (isTodayPost) {
            challenge.decreaseTodaySuccess();
        }

        // 해당 유저의 총 챌린지 성공 일수를 감소시킨다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(authorId, challenge.getId());
        challengeParticipant.removeDailySuccess(postDate);

        // 이번주 챌린지 총 성공 횟수를 감소시킨다.
        LocalDate thisWeekMonday = postDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ChallengeWeeklyStatus challengeWeeklyStatus = challengeQueryService.findWeeklyStatus(authorId, challenge.getId(), thisWeekMonday);
        challengeWeeklyStatus.decreaseSuccessCount(challenge.getRequiredWeeklyCount());

        if (isTodayPost) {  // 삭제 날짜가 생성 날짜와 같다면 기존과 동일하게 유저 통계의 내용을 롤백한다.
            userStatisticsService.onTodayPostCancelled(authorId, challenge.getId(), postDate);
        }
        else {
            /*
                과거 글: unmarkActiveToday는 슬라이딩 윈도우가 과거 날짜를 가정하지 않으므로 쓰지 않는다.
                로그 삭제·참가자 롤백 반영 후 남은 데일리 로그로 UserStatistics를 다시 맞춘 뒤 인증글을 삭제한다.
             */
            userStatisticsService.resyncUserStatisticsAfterPastPostDeletion(authorId);
            postRepository.delete(article);
            return;
        }

        postRepository.delete(article);  // DB에서 게시글을 삭제한다. 댓글이나 좋아요 등의 정보도 함께 삭제된다.
    }
}