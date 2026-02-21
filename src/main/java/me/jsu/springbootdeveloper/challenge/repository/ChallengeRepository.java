// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

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

    // 현재 진행중인 챌린지들을 배치 단위로 검색한다.
    @Query("""
        select c
        from Challenge c
        where c.startDate <= :today
          and c.endDate >= :today
          and c.id > :lastId
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
        order by c.startDate desc
    """)
    List<Challenge> findPublicChallenges(@Param("today") LocalDate today);
}
