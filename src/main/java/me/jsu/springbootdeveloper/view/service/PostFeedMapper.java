// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.post.dto.ArticleDetailResponse;
import me.jsu.springbootdeveloper.post.dto.ArticleImageResponse;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.post.service.PostLikeService;
import me.jsu.springbootdeveloper.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;

/*
    검색된 인증글들을 피드로 만들기 위해 매핑하는 클래스이다.
 */
@Component
@RequiredArgsConstructor
public class PostFeedMapper {
    private final PostService postService;
    private final PostLikeService postLikeService;
    private final ChallengeQueryService challengeQueryService;

    public Page<ArticleDetailResponse> toDto(Page<Article> articles, Long userId) {
        // 인증글의 부가 정보를 얻기 위해 인증글 id, 챌린지 id를 수집한다.
        List<Article> articleList = articles.getContent();

        List<Long> postIds = articleList.stream()  // 인증글 id를 수집한다.
                .map(Article::getId)
                .toList();

        List<Long> challengeIds = articleList.stream()
                .map(Article::getChallenge)
                .filter(Objects::nonNull)  // 챌린지가 null인 경우를 방어한다.
                .map(Challenge::getId)
                .distinct()  // 인증글 id는 고유이지만 챌린지 id는 fk값이므로 중복을 제거한다.
                .toList();

        // 인증글 이미지를 구한다.
        Map<Long, List<ArticleImageResponse>> imageMap = new HashMap<>();  // postId를 기준으로 이미지 링크를 묶는다.

        if (!postIds.isEmpty()) {
            for (Object[] row : postService.findImagesByPostIds(postIds)) {
                Long postId = (Long) row[0];  // 게시글 id
                Long imageId = (Long) row[1];  // 사진 id
                String url = (String) row[2];  // 사진 주소
                // postId가 없으면 새로운 리스트를 생성하여 반환한다. 있다면 원래 있던 것을 반환한다.
                imageMap.computeIfAbsent(postId, k -> new ArrayList<>())
                        // add로 postId에 해당하는 리스트에 ArticleImageResponse 객체를 넣는다.
                        .add(ArticleImageResponse.builder()
                                .imageId(imageId)
                                .imageUrl(url)
                                .build());
            }
        }

        // 인증글에 대해 사용자가 좋아요를 눌렀는지에 대한 여부를 검색한다.
        Set<Long> likedPostIds = postIds.isEmpty() ? Set.of() : new HashSet<>(postLikeService.findLikedPostIds(userId, postIds));

        // 인증글 성공 일수를 검색하기 위해 인증글 작성자의 id를 수집한다.
        List<Long> authorIds = articleList.stream()
                .map(Article::getUser)
                .filter(Objects::nonNull)  // 유저가 null인 경우를 방어한다.
                .map(User::getId)
                .distinct()
                .toList();

        // 인증글의 성공 일수를 검색한다.
        Map<Long, Map<Long, Integer>> successDaysMap = new HashMap<>();  // 조회의 편의를 위해 map을 사용한다.

        if (!authorIds.isEmpty() && !challengeIds.isEmpty()) {
            for (Object[] row : challengeQueryService.findSuccessDays(authorIds, challengeIds)) {
                Long authorId = (Long) row[0];
                Long challengeId = (Long) row[1];
                Integer days = (Integer) row[2];
                successDaysMap.computeIfAbsent(authorId, k -> new HashMap<>()).put(challengeId, days);  // 유저 id -> 챌린지 id -> 성공일수에 맞게 map을 완성한다.
            }
        }

        // DTO로 변환하여 반환한다.
        return articles.map(article -> {  // 반복문
            User author = article.getUser();
            Challenge challenge = article.getChallenge();

            // 챌린지 또는 유저가 null인 경우를 처리한다.
            Long authorId = (author == null) ? null : author.getId();
            Long challengeId = (challenge == null) ? null : challenge.getId();

            // 닉네임 처리
            String nickname = (author == null)
                    ? "탈퇴한 사용자"
                    : author.getNickname();

            // 프로필 사진 처리
            String profileUrl = null;
            if (author != null && author.getUserProfilePicture() != null) {
                profileUrl = author.getUserProfilePicture().getImageUrl();
            }

            // 성공일수 처리
            int successDays = 0;
            if (authorId != null && challengeId != null) {
                successDays = successDaysMap
                        .getOrDefault(authorId, Map.of())  // 글의 작성자 id로 map에 1차 접근을 한다.
                        .getOrDefault(challengeId, 0);  // 그 안에서 챌린지 id로 2차 접근을 한다. 값이 없으면 0을 사용한다.
            }

            return ArticleDetailResponse.builder()
                    .postId(article.getId())
                    .challengeTitle(
                            challenge == null ? "삭제된 챌린지" : challenge.getTitle()
                    )
                    .totalSuccessDays(successDays)
                    .content(article.getContent())
                    .images(
                            imageMap.getOrDefault(article.getId(), List.of())
                    )
                    .userNickname(nickname)
                    .userImageUrl(profileUrl)
                    .updatedAt(article.getUpdateAt())
                    .createdAt(article.getCreatedAt())
                    .isEdited(Boolean.TRUE.equals(article.getIsEdited()))
                    .likeNumber(article.getLikeNumber())
                    .commentNumber(article.getCommentNumber())
                    .liked(likedPostIds.contains(article.getId()))
                    .build();
        });
    }
}