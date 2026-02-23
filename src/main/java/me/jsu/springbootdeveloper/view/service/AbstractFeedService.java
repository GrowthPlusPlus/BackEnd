// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.service.PostQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.post.dto.article.ArticleDetailResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.ZoneId;

@RequiredArgsConstructor
public abstract class AbstractFeedService {
    protected final UserService userService;
    protected final PostQueryService postQueryService;
    protected final ChallengeService challengeService;
    protected final PostFeedMapper postFeedMapper;

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

        return postFeedMapper.toDto(articles, user.getId());
    }

    // 피드 조회 범위에 따라 상속받아 구현한다.
    protected abstract Page<Article> getArticles(Long userId, LocalDate targetDate, Pageable pageable);
}
