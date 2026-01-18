// 정성우

package me.jsu.springbootdeveloper.repository.post;

import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/*
    게시글을 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Article, 엔티티의 pk 타입은 Long이다.
 */
public interface PostRepository extends JpaRepository<Article, Long> {
    // 오늘 하루 유저의 모든 종류의 post를 조회하기 위한 쿼리문이다.
    List<Article> findByUser_IdAndPostDate(
            Long userId,
            LocalDate postDate
    );

    // 최신순으로 피드에 post를 보여주기 위한 쿼리문이다. page로 전체 게시글 중 일부를 보여준다.
    @Query("""
        select a
        from Article a
        join a.challenge c
        where c.challengeVisibility = :visibility
        order by a.createdAt desc
    """)
    Page<Article> findPublicChallengePostFeed(
            @Param("visibility") ChallengeVisibility visibility,
            Pageable pageable
    );

    // post 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
    @Modifying
    @Query("""
        update Article p
        set p.user = null
        where p.user = :user
    """)
    void detachUser(@Param("user") User user);

    // 유저 id, 챌린지 id로 날짜 2개 사이의 인증글의 id, 날짜, 사진을 검색해준다. 캘린더를 조회할때 사용한다.
    @Query("""
        select new me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse(
            p.id,
            p.postDate,
            i.imageUrl
        )
        from ChallengePost p
        left join p.images i
        where p.user.id = :userId
          and p.challenge.id = :challengeId
          and p.postDate between :start and :end
    """)
    List<ChallengeCalendarPhotoResponse> findCalendarPhotos(
            Long userId,
            Long challengeId,
            LocalDate start,
            LocalDate end
    );
}