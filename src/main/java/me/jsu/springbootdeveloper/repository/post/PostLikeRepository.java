// 정성우

package me.jsu.springbootdeveloper.repository.post;

import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.post.PostLike;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
    게시글에 달린 좋아요를 저장하는 DB이다.
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    // 유저 id와 게시글 id로 유저가 좋아요를 누른 여부를 반환한다.
    boolean existsByUserAndArticle (User user, Article article);
    List<PostLike> findByArticle_Id (Long articleId);  // 게시글 id로 좋아요를 누른 모든 유저를 검색한다.
}
