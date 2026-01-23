// 정성우

package me.jsu.springbootdeveloper.service.view;

import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import me.jsu.springbootdeveloper.service.post.PostLikeService;
import me.jsu.springbootdeveloper.service.post.PostService;
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
