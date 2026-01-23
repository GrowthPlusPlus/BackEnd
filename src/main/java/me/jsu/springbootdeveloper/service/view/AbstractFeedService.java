// 정성우

package me.jsu.springbootdeveloper.service.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.domain.user.UserProfilePicture;
import me.jsu.springbootdeveloper.dto.article.ArticleDetailResponse;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import me.jsu.springbootdeveloper.service.post.PostLikeService;
import me.jsu.springbootdeveloper.service.post.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public abstract class AbstractFeedService {
    protected final UserService userService;
    protected final PostService postService;
    protected final PostLikeService postLikeService;
    protected final ChallengeService challengeService;

    /*
        챌린지의 공개 상태에 따라 게시글 일부를 조회하는 함수로, 템플릿 메서드 패턴을 사용하기 위해 존재한다.
     */
    public Page<ArticleDetailResponse> findFeed(String email, int page) {
        User user = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 인증글 개수
                Sort.by("createdAt").descending()  // 인증일을 기준으로 내림차순 검색한다.
        );

        // 현재를 기준으로 챌린지가 진행 중인지 확인한다.
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 인증글 목록을 조회하는 부분으로, 템플릿 메서드의 대상이다.
        Page<Article> articles = getArticles(user.getId(), targetDate, pageable);

        // 인증글의 부가 정보를 얻기 위해 인증글 id, 챌린지 id를 수집한다.
        List<Article> articleList = articles.getContent();
        List<Long> postIds = articleList.stream()
                .map(Article::getId)
                .toList();
        List<Long> challengeIds = articleList.stream()
                .map(a -> a.getChallenge().getId())
                .distinct()  // 인증글 id는 고유이지만 챌린지 id는 fk값이므로 중복을 제거한다.
                .toList();

        // 인증글 이미지를 구한다.
        Map<Long, List<String>> imageMap = new HashMap<>();  // postId를 기준으로 이미지 링크를 묶는다.
        for (Object[] row : postService.findImagesByPostIds(postIds)) {
            Long postId = (Long) row[0];  // 게시글 id
            String url = (String) row[1];  // 게시글의 사진 중 하나
            // postId가 없으면 새로운 리스트를 생성하여 반환한다. 있다면 원래 있던 것을 반환한다.
            imageMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(url);  // add로 postId에 해당하는 리스트에 사진 url을 넣는다.
        }

        // 인증글에 대해 사용자가 좋아요를 눌렀는지에 대한 여부를 검색한다.
        Set<Long> likedPostIds = new HashSet<>(postLikeService.findLikedPostIds(user.getId(), postIds));

        // 인증글의 성공 일수를 검색한다.
        Map<Long, Integer> successDaysMap = new HashMap<>();  // 조회의 편의를 위해 map을 사용한다.
        for (Object[] row : challengeService.findSuccessDays(user.getId(), challengeIds)) {
            Long challengeId = (Long) row[0];
            Integer days = (Integer) row[1];
            successDaysMap.put(challengeId, days);  // 챌린지 id에 해당하는 성공 일수를 넣는다.
        }

        // DTO로 변환하여 반환한다.
        return articles.map(article -> {  // 반복문
            UserProfilePicture profile = article.getUser().getUserProfilePicture();  // 인증글 작성자의 사전을 구한다.

            return ArticleDetailResponse.builder()
                    .postId(article.getId())
                    .challengeTitle(article.getChallenge().getTitle())
                    .totalSuccessDays(
                            successDaysMap.getOrDefault(article.getChallenge().getId(), 0)
                    )
                    .content(article.getContent())
                    .articleImageUrl(
                            imageMap.getOrDefault(article.getId(), List.of())
                    )
                    .userNickname(article.getUser().getNickname())
                    .userImageUrl(
                            profile == null ? null : profile.getImageUrl()
                    )
                    .createdAt(article.getCreatedAt())
                    .likeNumber(article.getLikeNumber())
                    .commentNumber(article.getCommentNumber())
                    .liked(likedPostIds.contains(article.getId()))
                    .build();
        });
    }

    // 피드 조회 범위에 따라 상속받아 구현한다.
    protected abstract Page<Article> getArticles(Long userId, LocalDate targetDate, Pageable pageable);
}
