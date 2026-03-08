// 정성우

package me.jsu.springbootdeveloper.post.repository;

import me.jsu.springbootdeveloper.post.domain.ReportComment;
import me.jsu.springbootdeveloper.post.dto.report.ReportCommentDetailResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    신고한 댓글을 관리하는 리포지토리이다.
 */
public interface ReportCommentRepository extends JpaRepository<ReportComment, Long> {
    boolean existsByUser_IdAndComment_Id(Long userId, Long commentId);

    // 댓글 신고 상세 정보를 가져오는 쿼리문이다.
    @Query("""
    select new me.jsu.springbootdeveloper.post.dto.report.ReportCommentDetailResponse(
        r.id,
        u.id,
        u.nickname,
        c.id,
        c.contents,
        a.id,
        challenge.title,
        r.reportReason,
        r.detailReason
    )
    from ReportComment r
    join r.comment c
    join c.article a
    left join r.user u
    left join a.challenge challenge
    where c.id = :commentId
    order by r.createdAt desc
""")
    List<ReportCommentDetailResponse> findReportCommentDetails(@Param("commentId") Long commentId);

    /*
        유저가 삭제되는 경우, 신고 수를 건드리지 않기 위해 유저와 detach하는 쿼리문이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ReportComment rc
        set rc.user = null
        where rc.user.id in :userIds
    """)
    void detachUser(@Param("userIds") List<Long> userIds);
}
