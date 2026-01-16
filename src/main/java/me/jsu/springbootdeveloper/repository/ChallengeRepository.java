// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.Challenge;
import me.jsu.springbootdeveloper.domain.ChallengeVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.awt.print.Pageable;
import java.time.LocalDate;
import java.util.List;

/*
    챌린지를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Challenge, 엔티티의 pk 타입은 Long이다.
 */
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    // 공개 챌린지만 시작일 기준으로 최신순으로 보여준다.
    Page<Challenge> findByChallengeVisibilityOrderByStartDateDesc(
            ChallengeVisibility challengeVisibility,
            Pageable pageable
    );

    // 진행중인 챌린지를 검색하여 유저에게 보여준다.
    Page<Challenge> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate today1,  // today1, today2는 같은 값이다.
            LocalDate today2,
            Pageable pageable
    );

    // 종료된 챌린지를 검색하여 유저에게 보여준다.
    Page<Challenge> findByEndDateBefore(
            LocalDate today,
            Pageable pageable
    );

    // 유저가 관리하는 모든 챌린지를 검색한다.
    List<Challenge> findByUser_Id(Long userId);
}
