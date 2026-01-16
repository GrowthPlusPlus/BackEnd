// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
    게시글에 달리는 댓글을 저장하는 DB이다.
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByArticle_Id(Long articleId);  // 게시글에 달린 모든 댓글을 검색한다.
}
