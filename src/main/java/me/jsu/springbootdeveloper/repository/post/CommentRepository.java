// 정성우

package me.jsu.springbootdeveloper.repository.post;

import me.jsu.springbootdeveloper.domain.post.Comment;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    게시글에 달리는 댓글을 저장하는 DB이다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArticle_Id(Long articleId);  // 게시글에 달린 모든 댓글을 검색한다.

    /*
        댓글 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
     */
    @Modifying
    @Query("""
        update Comment c
        set c.user = null
        where c.user = :user
    """)
    void detachUser(@Param("user") User user);
}
