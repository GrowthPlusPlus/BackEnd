// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.dto.article.ArticleDetailResponse;
import me.jsu.springbootdeveloper.post.service.PostQueryService;
import me.jsu.springbootdeveloper.post.service.PostService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class MemberFeedService {
    private final UserService userService;
    private final PostQueryService postQueryService;
    private final PostFeedMapper postFeedMapper;

    /*
        챌린지 맴버들의 인증글을 조회한다.
     */
    public Page<ArticleDetailResponse> findMemberFeed(String email, Long challengeId, int page) {
        User user = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 인증글 개수
                Sort.by("createdAt").descending()  // 인증일을 기준으로 내림차순 검색한다.
        );

        // 현재를 기준으로 챌린지가 진행 중인지 확인한다.
        LocalDate targetDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 인증글 목록을 챌린지 id로 조회한다.
        Page<Article> articles = postQueryService.findChallengeFeed(challengeId, pageable);

        return postFeedMapper.toDto(articles, user.getId());  // dto로 가공하여 반환한다.
    }
}
