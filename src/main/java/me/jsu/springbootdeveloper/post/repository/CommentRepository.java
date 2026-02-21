// 정성우

package me.jsu.springbootdeveloper.post.repository;

import me.jsu.springbootdeveloper.post.domain.Comment;
import me.jsu.springbootdeveloper.post.dto.ReportedCommentResponse;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Comment c
        set c.user = null
        where c.user = :user
    """)
    void detachUser(@Param("user") User user);

    // 댓글과 댓글을 작성한 유저, 유저의 프로필 사진을 함께 조회하는 쿼리문이다.
    @Query(
        value = """
            select c
            from Comment c
            join fetch c.user u
            left join fetch u.userProfilePicture upp
            where c.article.id = :postId
        """,
        countQuery = """
            select count(c)
            from Comment c
            where c.article.id = :postId
        """
    )
    Page<Comment> findByArticleIdWithUser(
            @Param("postId") Long postId,
            Pageable pageable
    );

    // 신고된 댓글들을 한 번에 조회한다.
    @Query("""
        select new me.jsu.springbootdeveloper.post.dto.ReportedCommentResponse(
            c.id,
            challenge.title,
            COALESCE(a.totalReport, 0)
        )
        from Comment c
        join c.article a
        left join a.challenge challenge
        where c.totalReport > 0
        order by c.totalReport desc
    """)
    List<ReportedCommentResponse> findReportedComments();
}
