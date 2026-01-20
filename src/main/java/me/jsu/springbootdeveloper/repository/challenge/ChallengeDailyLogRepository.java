// 정성우

package me.jsu.springbootdeveloper.repository.challenge;

import me.jsu.springbootdeveloper.domain.challenge.ChallengeDailyLog;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지 로그를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 ChallengeDailyLog, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeDailyLogRepository extends JpaRepository<ChallengeDailyLog, Long> {
    List<ChallengeDailyLog> findByUser_IdAndPostDateBetweenOrderByPostDate(  // 한 달치 로그를 조회하기 위한 쿼리문이다.
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    // 유저의 오늘 하루의 post 로그를 조회하기 위한 쿼리문이다.
    List<ChallengeDailyLog> findByUser_IdAndPostDate(
            Long userId,
            LocalDate postDate
    );

    /*
        챌린지 데일리 로그 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
        즉 회원탈퇴 시에 데일리 로그 정보가 삭제되지 않도록 fk값을 null로 만드는 역할을 한다.
        쿼리문은 기본적으로 조회문이기 때문에 update를 하기 위해 @Modifying을 사용한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChallengeDailyLog log
        set log.user = null
        where log.user = :user
    """)
    void detachUser(@Param("user") User user);

    /*
        챌린지 데일리 로그 테이블에서 특정 article인 행의 article_id 칼럼을 null로 바꾸는 쿼리문이다.
        게시글 삭제 시에 로그 정보가 삭제되지 않도록 fk 값을 null로 만드는 역할을 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChallengeDailyLog log
        set log.article = null
        where log.article = :article
    """)
    void detachArticle(@Param("article") Article article);
}
