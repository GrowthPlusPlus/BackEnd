// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.domain.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/*
    게시글을 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Article, 엔티티의 pk 타입은 Long이다.
 */
public interface PostRepository extends JpaRepository<Article, Long> {
    // 오늘 하루 유저의 모든 종류의 post를 조회하기 위한 쿼리문이다.
    List<Article> findByUserAndPostDate(
            User user,
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
}
