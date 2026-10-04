// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import lombok.NonNull;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Challenge, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    // 유저가 방장인 모든 챌린지를 검색한다.
    List<Challenge> findByUser_Id(Long userId);

    // 챌린지 id와 유저 id로 해당 유저가 방장인지 검사한다.
    boolean existsByIdAndUser_Id(Long challengeId, Long userId);

    /**
     * 제목 또는 연관된 태그 이름에 키워드가 포함된 공개 챌린지를 검색한다.
     * @param keyword 검색어
     * @param visibility 공개 여부
     * @param pageable 페이징 정보
     */
    @Query("SELECT DISTINCT c FROM Challenge c " +
            "LEFT JOIN c.tagMappings tm " +
            "LEFT JOIN tm.tag t " +
            "WHERE c.challengeVisibility = :visibility " +
            "AND (c.title LIKE %:keyword% OR t.tagName LIKE %:keyword%) " +
            "ORDER BY c.startDate DESC")
    Page<Challenge> searchChallenges(
            @Param("keyword") String keyword,
            @Param("visibility") ChallengeVisibility visibility,
            Pageable pageable
    );

    /*
        제목 또는 태그로 공개 챌린지를 검색한다.
        누구든 완료(SUCCESS)한 챌린지는 결과에서 뺀다.
        현재 사용자가 실패(FAIL)한 챌린지도 결과에서 뺀다.

        @param keyword 제목 또는 태그 검색어이다.
        @param visibility 공개 여부이다.
        @param userId 현재 사용자 id이다. 비로그인이면 null이다.
        @param pageable 페이징 정보이다.
        @return 완료된 챌린지와 내 실패 챌린지를 제외한 검색 페이지이다.
     */
    @Query(
            value = """
                select distinct c
                from Challenge c
                left join c.tagMappings tm
                left join tm.tag t
                where c.challengeVisibility = :visibility
                  and (c.title like %:keyword% or t.tagName like %:keyword%)
                  and not exists (
                      select finished.id
                      from ChallengeParticipant finished
                      where finished.challenge = c
                        and finished.status = me.jsu.springbootdeveloper.challenge.enumType.Status.SUCCESS
                  )
                  and (
                      :userId is null
                      or not exists (
                          select mine.id
                          from ChallengeParticipant mine
                          where mine.challenge = c
                            and mine.user.id = :userId
                            and mine.status = me.jsu.springbootdeveloper.challenge.enumType.Status.FAIL
                      )
                  )
                order by c.startDate desc
                """,
            countQuery = """
                select count(distinct c.id)
                from Challenge c
                left join c.tagMappings tm
                left join tm.tag t
                where c.challengeVisibility = :visibility
                  and (c.title like %:keyword% or t.tagName like %:keyword%)
                  and not exists (
                      select finished.id
                      from ChallengeParticipant finished
                      where finished.challenge = c
                        and finished.status = me.jsu.springbootdeveloper.challenge.enumType.Status.SUCCESS
                  )
                  and (
                      :userId is null
                      or not exists (
                          select mine.id
                          from ChallengeParticipant mine
                          where mine.challenge = c
                            and mine.user.id = :userId
                            and mine.status = me.jsu.springbootdeveloper.challenge.enumType.Status.FAIL
                      )
                  )
                """
    )
    Page<Challenge> searchChallengesExcludingFinishedByUser(
            @Param("keyword") String keyword,
            @Param("visibility") ChallengeVisibility visibility,
            @Param("userId") Long userId,
            Pageable pageable
    );

    // 현재 진행중인 챌린지들을 배치 단위로 검색한다.
    @Query("""
        select c
        from Challenge c
        where c.startDate <= :today
          and c.endDate >= :today
          and c.id > :lastId
          and c.active = true
        order by c.id asc
    """)
    List<Challenge> findActiveChallengesAfterId(@Param("today") LocalDate today, @Param("lastId") Long lastId, Pageable pageable);

    // 공개 챌린지를 검색한다. AI 추천 기능에서 사용한다.
    @Query("""
        select c
        from Challenge c
        where c.challengeVisibility = 'PUBLIC'
          and c.startDate <= :today
          and c.endDate >= :today
          and c.active = true
        order by c.startDate desc
    """)
    List<Challenge> findPublicChallenges(@Param("today") LocalDate today);

    /*
        진행 중인 공개 챌린지와 태그 매핑·태그 엔티티를 한 번에 조회한다.
        탐색 탭 추천 API에서 N+1 조회를 막기 위해 사용한다.
     */
    @Query("""
        select distinct c
        from Challenge c
        left join fetch c.tagMappings tm
        left join fetch tm.tag
        where c.challengeVisibility = 'PUBLIC'
          and c.startDate <= :today
          and c.endDate >= :today
          and c.active = true
        """)
    List<Challenge> findPublicChallengesWithTagMappings(@Param("today") LocalDate today);

    boolean existsById(@NonNull Long Id);
}
