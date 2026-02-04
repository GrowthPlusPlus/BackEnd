// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.post.dto.ArticleImageResponse;
import me.jsu.springbootdeveloper.post.exception.ArticleAccessDeniedException;
import me.jsu.springbootdeveloper.post.exception.ArticleNotFoundException;
import me.jsu.springbootdeveloper.post.exception.DuplicateDailyPostException;
import me.jsu.springbootdeveloper.post.exception.InvalidImageException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.post.domain.PostPicture;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.post.dto.ArticleDetailResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.post.repository.PostPictureRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final PostPictureRepository postPictureRepository;
    private final CloudinaryService cloudinaryService;
    private static final int MAX_IMAGE_COUNT = 3;

    /*
        게시글을 추가하는 함수이다.
        유저로부터 받은 이미지는 검증 서비스를 거치고 왔다고 가정하고 수행한다.
     */
    @Transactional
    public ArticleDetailResponse save(String email, String content, Long challengeId, List<MultipartFile> images) {
        // 유저 이메일로 user 객체를 검색한다.
        User user = userService.findByEmail(email);

        // request로 온 챌린지 id로 post가 어떤 챌린지에 속하는지 검색한다.
        Challenge challenge = challengeService.findById(challengeId);

        // 현재 진행중인 챌린지가 아니라면 예외를 던진다.
        challengeService.challengeStatusInProgress(user.getId(), challengeId);

        // 챌린지 인증 시각을 정확하게 보기 위해 생성 시각과는 다른 인증 날짜 도메인을 생성한다.
        LocalDate postDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 하루 1회로 인증을 제한한다.
        boolean alreadyPosted = postRepository.existsByUser_IdAndChallenge_IdAndPostDate(user.getId(), challenge.getId(), postDate);
        if (alreadyPosted) {
            throw new DuplicateDailyPostException("DAILY_POST_ALREADY_EXISTS");
        }

        // 이미지가 아에 비어있으면 빈 리스트를 대입한다.
        images = (images == null) ? List.of() : images;

        // 챌린지의 이미지 필수 여부를 확인한다.
        boolean photoRequired = challenge.isPhotoRequired();
        if (photoRequired && images.isEmpty()) {
            throw new InvalidImageException("IMAGE_REQUIRED");  // 사진이 필수 인증인데 이미지가 없다면 예외처리 한다.
        }

        // 업로드 한 사진의 개수가 5개보다 많으면 예외처리 한다.
        if (images.size() > MAX_IMAGE_COUNT) {
            throw new InvalidImageException("MAX_IMAGE_COUNT_EXCEEDED");
        }

        // 서버에 글을 저장한다.
        Article article = postRepository.save(Article.builder()
                .content(content)
                .user(user)
                .challenge(challenge)
                .postDate(postDate)
                .build());

        // 검증된 사진을 DB와 클라우디너리에 저장한다.
        saveImages(images, article);

        // 인증글을 업데이트 했으므로 총 인증 횟수와 연속 인증 횟수를 업데이트 한다.
        challengeService.succeedChallenge(user.getId(), challengeId);

        // 인증글을 업데이트 했으므로 챌린지 데일리 로그도 업데이트 한다.
        challengeService.createDailyLog(user, challenge, article, postDate);

        // 오늘 인증한 사람 수를 증가시킨다.
        challengeService.updateTodaySuccessCount(challenge, postDate);

        // 주의 시작인 월요일을 계산한다.
        LocalDate weekStartDate = postDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 인증글을 업데이트 했으므로 챌린지 주간 상태도 업데이트 한다.
        challengeService.updateWeeklyStatus(user, challenge, weekStartDate, challenge.getRequiredWeeklyCount());

        // 저장한 인증글 id로 검색하여 인증글 상세 정보를 반환한다.
        return getPost(email, article.getId());
    }

    /*
        클라우디너리에 사진을 저장하고, 사진 정보를 DB에 반영해주는 함수이다.
     */
    public void saveImages(List<MultipartFile> images, Article article) {
        // 이미지가 없는 경우엔 if문을 실행하지 않는다.
        if (!images.isEmpty()) {
            for (MultipartFile multipartFile : images) {
                // 인증글 이미지를 클라우디너리에 업로드하고 정보를 반환받는다.
                ImageUploadResult imageUploadResult = cloudinaryService.uploadImage(multipartFile);

                // 사진 정보를 DB에 반영한다.
                postPictureRepository.save(PostPicture.builder()
                        .article(article)
                        .cloudinaryPublicId(imageUploadResult.publicId())
                        .imageUrl(imageUploadResult.secureUrl())
                        .build());
            }
        }
    }

    /*
        클라우디너리의 사진을 삭제하고, 사진 정보를 DB에서 삭제하는 함수이다.
     */
    public void deleteImages(List<PostPicture> postPictures) {
        // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다. 이미지가 없는 경우엔 if문을 실행하지 않는다.
        if (!postPictures.isEmpty()) {
            for (PostPicture pp : postPictures) {
                // 클라우디너리의 이미지를 삭제한다.
                cloudinaryService.deleteImage(pp.getCloudinaryPublicId());

                // 우리 서버의 DB에서도 정보를 삭제한다.
                postPictureRepository.delete(pp);
            }
        }
    }

    /*
        공개 인증글 목록을 조회한다.
        오늘 날짜, 페이지 정보를 넘겨준다.
        피드 생성에 사용된다.
     */
    public Page<Article> findPublicFeed(LocalDate targetDate, Pageable pageable) {
        return postRepository.findPublicFeed(targetDate, pageable);
    }

    /*
        친구 인증글 목록을 조회한다.
        자신의 id, 오늘 날짜, 페이지 정보를 넘겨준다.
        피드 생성에 사용된다.
     */
    public Page<Article> findFriendFeed(Long userId, LocalDate targetDate, Pageable pageable) {
        return postRepository.findFriendFeed(userId, targetDate, pageable);
    }

    /*
        인증글 id 여러 개로 그 인증글과 관련된 이미지들을 구한다.
        피드 생성에 사용된다.
     */
    public List<Object[]> findImagesByPostIds(List<Long> postIds) {
        return postPictureRepository.findImagesByPostIds(postIds);
    }

    /*
        게시글 단일 정보를 게시글 id로 조회하는 함수이다.
     */
    public Article findById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));
    }

    /*
        게시글 상제 정보를 게시글 id로 조회하는 함수이다.
     */
    public ArticleDetailResponse getPost(String email, Long postId) {
        User user = userService.findByEmail(email);

        Article article = postRepository.findWithUserAndProfileById(postId)  // 인증글과 작성자 정보, 사진을 함께 조회한다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        // 해당 챌린지의 총 인증 횟수를 구한다.
        ChallengeParticipant challengeParticipant = challengeService.findByUser_IdAndChallenge_IdForParticipant(
                article.getUser().getId(), article.getChallenge().getId());
        int totalSuccessDays = challengeParticipant.getTotalSuccessDays();

        // 작성자의 프로필 사진이 없는 경우를 대비한다.
        String userImageUrl;
        UserProfilePicture userProfilePicture = article.getUser().getUserProfilePicture();
        if (userProfilePicture == null) {
            userImageUrl = null;
        }
        else {
            userImageUrl = userProfilePicture.getImageUrl();
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
                .challengeTitle(article.getChallenge().getTitle())
                .totalSuccessDays(totalSuccessDays)  // 총 성공 일수이다.
                .content(article.getContent())
                .images(images)  // 게시글의 사진, id이다.
                .userNickname(article.getUser().getNickname())
                .userImageUrl(userImageUrl)  // 게시글 작성자의 사진이다.
                .createdAt(article.getCreatedAt())
                .likeNumber(article.getLikeNumber())  // 좋아요 개수이다.
                .commentNumber(article.getCommentNumber())  // 댓글 개수이다.
                .liked(liked)
                .build();
    }

    /*
        게시글 하나를 삭제하는 함수로, 게시글 작성자 id가 요청자 id와 다르면 삭제하지 않는다.

        다음은 @Transactional를 사용했을 때의 효과이다.
        1) findById() 호출 시 엔티티는 영속성 컨텍스트에 등록되어
           JPA의 관리 대상이 된다.

        2) 엔티티의 필드를 변경하면 JPA는 즉시 DB에 반영하지 않고,
           영속성 컨텍스트 내에서 변경된 상태를 유지한다.

        3) 트랜잭션 커밋 시점에 flush가 발생하고,
           JPA는 스냅샷과 현재 상태를 비교(dirty checking)하여
           변경된 엔티티에 대해 UPDATE SQL을 생성하고 실행한다.

        따라서 @Transactional는 DB 작업의 원자성, 일관성, 격리성, 지속성을 보장한다.
     */
    @Transactional
    public void delete(String email, Long postId) {
        User user = userService.findByEmail(email);

        Article article = postRepository.findById(postId)  // 삭제할 글을 찾는다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        // 글에 있는 사진을 찾는다.
        List<PostPicture> postPictures = postPictureRepository.findAllByArticle_Id(postId);

        authorizeArticleAuthor(article, user);  // 글 작성자인지 확인한다.

        // 챌린지 데일리 로그 정보는 게시글이 삭제되면 삭제한다.
        ChallengeDailyLog challengeDailyLog = challengeService.findLogByUser_IdAndArticle_IdAndPostDate(user.getId(), postId, article.getPostDate());
        challengeService.deleteLog(challengeDailyLog);

        deleteImages(postPictures);  // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다.

        // 챌린지의 오늘 성공 횟수를 감소시킨다.
        Challenge challenge = challengeService.findById(article.getChallenge().getId());
        challenge.decreaseTodaySuccess();

        // 해당 유저의 총 챌린지 성공 일수를 감소시킨다.
        ChallengeParticipant challengeParticipant = challengeService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challenge.getId());
        challengeParticipant.removeDailySuccess(article.getPostDate());

        // 이번주 챌린지 총 성공 횟수를 감소시킨다.
        LocalDate thisWeekMonday = article.getPostDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ChallengeWeeklyStatus challengeWeeklyStatus = challengeService.findWeeklyStatus(user.getId(), challenge.getId(), thisWeekMonday);
        challengeWeeklyStatus.decreaseSuccessCount(challenge.getRequiredWeeklyCount());

        postRepository.delete(article);  // DB에서 게시글을 삭제한다. 댓글이나 좋아요 등의 정보도 함께 삭제된다.
    }

    /*
        인증글을 수정하는 함수이다.
        수정하는 값은 인증 내용, 사진이다.
     */
    @Transactional
    public ArticleDetailResponse update(String email, Long postId, String content, List<Long> deleteImageIds, List<MultipartFile> images) {
        User user = userService.findByEmail(email);
        Article article = findById(postId);

        authorizeArticleAuthor(article, user);  // 글 작성자인지 확인한다.

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

        int currentCount = article.getPostPictures().size();  // 기존 사진 개수
        int deleteCount = deleteImageIds.size();  // 삭제할 사진 개수
        int newCount = images.size();  // 새로 추가할 사진 개수

        int finalCount = currentCount - deleteCount + newCount;  // 최종 사진 개수

        if (finalCount > MAX_IMAGE_COUNT) {
            throw new InvalidImageException("MAX_IMAGE_COUNT_EXCEEDED");
        }

        // 인증글 내용을 업데이트 한다.
        article.updateContent(content);

        // 인증글의 기존 이미지를 삭제한다.
        deleteImages(postPictures);

        // 인증글의 사진을 업데이트 한다.
        saveImages(images, article);

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
    public void detachPostToUser(User user) {
        postRepository.detachUser(user);
    }


    /*
        캘린더를 조회하기 위해 사용하는 함수이다.
        DB 검색에서 자동으로 DTO를 생성하여 반환한다.
     */
    public List<ChallengeCalendarPhotoResponse> findCalendarPhotos(Long userId, Long challengeId, LocalDate start, LocalDate end) {
        return postRepository.findCalendarPhotos(userId, challengeId, start, end);
    }

    /*
        특정 챌린지의 해당 연 월에 내가 올린 인증글을 최신순으로 조회하여 반환한다.
        캘린더 뷰 화면에 필요한 정보이다.
     */
    public Page<ChallengeCalendarPostResponse> findCalendarPosts(Long userId, Long challengeId, LocalDate start, LocalDate end, Pageable pageable) {
        return postRepository.findCalendarPosts(userId, challengeId, start, end, pageable);
    }
}
