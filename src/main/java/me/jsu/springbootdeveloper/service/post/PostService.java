// 정성우

package me.jsu.springbootdeveloper.service.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.post.PostPicture;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.domain.user.UserProfilePicture;
import me.jsu.springbootdeveloper.dto.article.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.article.ArticleDetailResponse;
import me.jsu.springbootdeveloper.dto.article.UpdateArticleRequest;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.dto.picture.ImageUploadResult;
import me.jsu.springbootdeveloper.repository.post.PostLikeRepository;
import me.jsu.springbootdeveloper.repository.post.PostPictureRepository;
import me.jsu.springbootdeveloper.repository.post.PostRepository;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import me.jsu.springbootdeveloper.service.cloudinary.CloudinaryService;
import me.jsu.springbootdeveloper.service.cloudinary.CloudinaryUploadException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final ChallengeService challengeService;
    private final PostLikeRepository postLikeRepository;
    private final PostPictureRepository postPictureRepository;
    private final CloudinaryService cloudinaryService;
    private static final int MAX_IMAGE_COUNT = 5;

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

        // 챌린지 인증 시각을 정확하게 보기 위해 생성 시각과는 다른 인증 날짜 도메인을 생성한다.
        LocalDate postDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 하루 1회로 인증을 제한한다.
        boolean alreadyPosted = postRepository.existsByUser_IdAndChallenge_IdAndPostDate(user.getId(), challenge.getId(), postDate);
        if (alreadyPosted) {
            throw new DuplicateDailyPostException("DAILY_POST_ALREADY_EXISTS");
        }

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

        // 검증된 사진을 DB와 클라우디너리에 저장한다. 이미지가 없는 경우엔 if문을 실행하지 않는다.
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

        // 인증글을 업데이트 했으므로 총 인증 횟수와 연속 인증 횟수를 업데이트 한다.
        challengeService.succeedChallenge(article.getId(), challengeId);

        // 인증글을 업데이트 했으므로 챌린지 데일리 로그도 업데이트 한다.
        challengeService.createDailyLog(user, challenge, article, postDate);

        // 주의 시작인 월요일을 계산한다.
        LocalDate weekStartDate = postDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 인증글을 업데이트 했으므로 챌린지 주간 상태도 업데이트 한다.
        challengeService.updateWeeklyStatus(user, challenge, weekStartDate, challenge.getRequiredWeeklyCount());

        // 저장한 인증글 id로 검색하여 인증글 상세 정보를 반환한다.
        return getPost(email, article.getId());
    }

    /*
        공개 상태의 게시글 일부를 조회하는 함수이다.
        리턴하는 값은 Article 객체를 가지고 있는 리스트이다.
     */
    public Page<Article> findAll(Pageable pageable) {
        return postRepository.findPublicChallengePostFeed(ChallengeVisibility.PUBLIC, pageable);
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

        // 인증글 id로 인증글의 사진을 검색한다.
        List<String> articlePicture = postPictureRepository.findImageUrlsByPostId(postId);

        // 게시글을 조회한 유저가 해당 게시글에 좋아요를 눌렀는지에 대한 여부를 검색한다.
        boolean liked = postLikeRepository.existsByArticle_IdAndUser_Id(postId, user.getId());

        return ArticleDetailResponse.builder()
                .challengeTitle(article.getChallenge().getTitle())
                .totalSuccessDays(totalSuccessDays)  // 총 성공 일수이다.
                .content(article.getContent())
                .articleImageUrl(articlePicture)  // 게시글의 사진이다.
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
    public void delete(Long postId) {
        Article article = postRepository.findByIdWithImages(postId)  // 삭제할 글과 사진을 DB에서 찾는다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        authorizeArticleAuthor(article);  // 글 작성자인지 확인한다.
        challengeService.detachLogToArticle(article);  // 챌린지 데일리 로그 정보는 게시글이 삭제되도 남아있도록 한다

        // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다.
        for (PostPicture postPicture : article.getPostPictures())
        {
            cloudinaryService.deleteImage(postPicture.getCloudinaryPublicId());
        }
        postRepository.delete(article);  // DB에서 게시글을 삭제한다. 댓글이나 좋아요 등의 정보도 함께 삭제된다.
    }

    /*
        게시글을 수정하는 함수로, 게시글 작성자 id가 요청자 id와 다르면 수정하지 않는다.
        해당 함수는 수정이 필요하다.
     */
    @Transactional
    public Article update(long id, UpdateArticleRequest request) {
        Article article = postRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        authorizeArticleAuthor(article);  // 글 작성자인지 확인한다.
        // 위에서 찾은 게시글을 UpdateArticleRequest 객체의 값으로 업데이트 한다.
        article.update(request.getTitle(), request.getContent());

        // @Transactional가 엔티티 수정을 감지하고 자동으로 DB에 값을 업데이트 하므로, 수정된 게시글 정보만 반환한다.
        return article;
    }

    /*
        계시글을 작성한 유저인지 확인해주는 함수이다.
        email을 기준으로 비교한다.
     */
    private static void authorizeArticleAuthor(Article article) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();  // 현재 로그인한 유저의 이메일을 가져온다.

        if (!article.getUser().getEmail().equals(userName)) {  // 유저의 이메일과 게시글 작성자의 이메일을 비교한다.
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
