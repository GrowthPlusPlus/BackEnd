// 정성우

package me.jsu.springbootdeveloper.repository.challenge;

import me.jsu.springbootdeveloper.domain.challenge.ChallengeDailyLog;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
