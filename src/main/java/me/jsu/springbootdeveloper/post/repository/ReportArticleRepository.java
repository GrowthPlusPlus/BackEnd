// 정성우

package me.jsu.springbootdeveloper.post.repository;

import me.jsu.springbootdeveloper.post.domain.ReportArticle;
import me.jsu.springbootdeveloper.post.dto.ReportArticleDetailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    신고한 인증글을 관리하는 리포지토리이다.
 */
public interface ReportArticleRepository extends JpaRepository<ReportArticle, Long> {
    boolean existsByUser_IdAndArticle_Id(Long userId, Long articleId);

    // 인증글 신고 상세 정보를 가져오는 쿼리문이다.
    @Query("""
    select new me.jsu.springbootdeveloper.post.dto.ReportArticleDetailResponse(
        r.id,
        u.id,
        u.nickname,
        c.title,
        a.id,
        a.content,
        i.imageUrl,
        r.reportReason,
        r.detailReason
    )
    from ReportArticle r
    join r.article a
    left join r.user u
    left join a.challenge c
    left join PostPicture i
        on i.article = a
       and i.id = (
            select min(pp.id)
            from PostPicture pp
            where pp.article = a
       )
    where a.id = :articleId
    order by r.createdAt desc
""")
    List<ReportArticleDetailResponse> findReportArticleDetails(@Param("articleId") Long articleId);

    /*
        유저가 삭제되는 경우, 신고 수를 건드리지 않기 위해 유저와 detach하는 쿼리문이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ReportArticle ra
        set ra.user = null
        where ra.user.id = :userId
    """)
    void detachUser(@Param("userId") Long userId);
}