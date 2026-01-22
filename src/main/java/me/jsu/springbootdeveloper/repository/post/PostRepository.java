// 정성우

package me.jsu.springbootdeveloper.repository.post;

import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Article p
        set p.user = null
        where p.user = :user
    """)
    void detachUser(@Param("user") User user);

    // 유저 id, 챌린지 id로 날짜 2개 사이의 인증글의 id, 날짜, 사진을 검색해준다. 캘린더를 조회할때 사용한다.
    @Query("""
        select new me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse(
            p.id,
            p.postDate,
            i.imageUrl
        )
        from Article p
        left join PostPicture i on i.article = p
        where p.user.id = :userId
          and p.challenge.id = :challengeId
          and p.postDate between :start and :end
    """)
    List<ChallengeCalendarPhotoResponse> findCalendarPhotos(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /*
        유저 id, 챌린지 id로 유저의 인증글을 검색한다.
        페이지 형태로 dto가공해 반환하여 해당 유저의 최신 인증글을 볼 수 있도록 한다.
        countQuery는 다른 테이블과 join하면서 page형태로 반환하는 경우에 정확한 page 계산을 위하여 사용한다.
     */
    @Query(
            value = """
            select new me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse(
                p.id,
                p.postDate,
                i.imageUrl,
                p.content
            )
            from Article p
            left join PostPicture i on i.article = p
            where p.user.id = :userId
              and p.challenge.id = :challengeId
              and p.postDate between :start and :end
        """,
        countQuery = """
            select count(p)
            from Article p
            where p.user.id = :userId
              and p.challenge.id = :challengeId
              and p.postDate between :start and :end
        """
    )
    Page<ChallengeCalendarPostResponse> findCalendarPosts(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            Pageable pageable
    );

    // 인증글 id로 인증글 정보 뿐만 아니라 작성자 정보, 작성자의 프로필 사진까지 한 번에 조회하는 쿼리문이다.
    @Query("""
        select p
        from Article p
        join fetch p.user u
        left join fetch u.userProfilePicture
        where p.id = :articleId
    """)
    Optional<Article> findWithUserAndProfileById(@Param("articleId") Long postId);

    // 인증글과 인증글 사진을 함께 조회한다.
    @Query("""
        select distinct a
        from Article a
        left join fetch a.postPictures
        where a.id = :articleId
    """)
    Optional<Article> findByIdWithImages(@Param("articleId") Long postId);

    /*
        인증글 테이블에서 특정 challenge인 행의 challenge_id 칼럼을 null로 바꾸는 쿼리문이다.
        챌린지 삭제 시에 인증글이 삭제되지 않도록 fk 값을 null로 만드는 역할을 한다.
     */
    @Modifying
    @Query("""
        update Article a
        set a.challenge = null
        where a.challenge = :challenge
    """)
    void detachChallenge(@Param("challenge") Challenge challenge);

    // 유저 id, 챌린지 id, 인증 날짜로 같은 날에 두 번 인증하는지 검사한다.
    boolean existsByUser_IdAndChallenge_IdAndPostDate(Long userId, Long challengeId, LocalDate postDate);
}