// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*
    챌린지 로그를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 ChallengeDailyLog, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeDailyLogRepository extends JpaRepository<ChallengeDailyLog, Long> {
    // 특정 유저가 특정 날짜에 하나라도 인증했는지 판단하는 쿼리문이다.
    boolean existsByUser_IdAndPostDate(Long userId, LocalDate postDate);

    // 유저의 오늘 하루의 post 로그를 조회하기 위한 쿼리문이다. 챌린지가 없는 경우 제외한다.
    @Query("""
        select cdl
        from ChallengeDailyLog cdl
        join fetch cdl.challenge ch
        where cdl.user.id = :userId
          and cdl.postDate = :postDate
          and cdl.challenge is not null
    """)
    List<ChallengeDailyLog> findByUser_IdAndPostDate(
            @Param("userId") Long userId,
            @Param("postDate") LocalDate postDate
    );

    // 유저의 로그를 날짜와 인증글 id로 검색한다.
    @Query("""
        select cdl
        from ChallengeDailyLog cdl
        where cdl.user.id = :userId
          and cdl.article.id = :articleId
          and cdl.postDate = :postDate
    """)
    Optional<ChallengeDailyLog> findByUser_IdAndArticle_IdAndPostDate(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId,
            @Param("postDate") LocalDate postDate
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

    /*
        챌린지 데일리 로그 테이블에서 특정 challenge인 행의 challenge_id 칼럼을 null로 바꾸는 쿼리문이다.
        챌린지 삭제 시에 로그 정보가 삭제되지 않도록 fk 값을 null로 만드는 역할을 한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChallengeDailyLog log
        set log.challenge = null
        where log.challenge = :challenge
    """)
    void detachChallenge(@Param("challenge") Challenge challenge);

    /*
        오늘 해당 챌린지를 수행한 유저의 로그를 검색하여 유저 정보를 얻는다.
     */
    @Query("""
        select log.user
        from ChallengeDailyLog log
        where log.challenge.id = :challengeId
          and log.postDate = :today
    """)
    List<User> findTodaySuccessUsers(
            @Param("challengeId") Long challengeId,
            @Param("today") LocalDate today
    );
}
