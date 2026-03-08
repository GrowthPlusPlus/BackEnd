// 정성우

package me.jsu.springbootdeveloper.post.repository;

import me.jsu.springbootdeveloper.post.domain.PostLike;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    게시글에 달린 좋아요를 저장하는 DB이다.
 */
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    // 유저 id와 게시글 id로 유저가 좋아요를 누른 여부를 반환한다.
    boolean existsByArticle_IdAndUser_Id(Long articleId, Long userId);

    // 유저 id와 게시글 id 여러 개로 유저가 좋아요를 누른 여부를 반환한다. 반환되는 값은 좋아요를 누른 인증글의 id이다.
    @Query("""
        select pl.article.id
        from PostLike pl
        where pl.user.id = :userId
          and pl.article.id in :postIds
    """)
    List<Long> findLikedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds
    );

    List<PostLike> findByArticle_Id (Long articleId);  // 게시글 id로 좋아요를 누른 모든 유저를 검색한다.

    /*
        게시글 좋아요 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PostLike pl
        set pl.user = null
        where pl.user.id in :userIds
    """)
    void detachUser(@Param("userIds") List<Long> userIds);

    // 인증글 id와 유저 id로 좋아요를 검색한다.
    Optional<PostLike> findByArticle_IdAndUser_Id(Long articleId, Long userId);
}
