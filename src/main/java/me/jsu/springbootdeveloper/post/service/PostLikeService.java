// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.PostLike;
import me.jsu.springbootdeveloper.post.exception.ArticleNotFoundException;
import me.jsu.springbootdeveloper.post.exception.DuplicatePostLikeException;
import me.jsu.springbootdeveloper.post.exception.PostLikeNotFoundException;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.post.repository.PostLikeRepository;
import org.springframework.stereotype.Service;

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

    /*
        게시글 좋아요의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachLikeToUser(User user) {
        postLikeRepository.detachUser(user);
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

        //좋아요 눌렀을때 푸시 알림 전송

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
