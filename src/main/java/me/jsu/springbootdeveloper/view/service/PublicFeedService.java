// 정성우

package me.jsu.springbootdeveloper.view.service;

import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.post.service.PostLikeService;
import me.jsu.springbootdeveloper.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/*
    피드 중, 공개 범위의 피드를 검색하는 서비스이다.
    AbstractFeedService를 상속받아 사용한다.
 */
@Service
public class PublicFeedService extends AbstractFeedService {
    public PublicFeedService(UserService userService, PostService postService, PostLikeService postLikeService, ChallengeService challengeService) {
        super(userService, postService, postLikeService, challengeService);
    }

    @Override
    protected Page<Article> getArticles(Long userId, LocalDate targetDate, Pageable pageable) {
        return postService.findPublicFeed(targetDate, pageable);
    }
}
