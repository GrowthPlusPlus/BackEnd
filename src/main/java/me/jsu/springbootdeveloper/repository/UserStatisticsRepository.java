// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    유저의 통계 정보를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 UserStatistics, 엔티티의 pk 타입은 Long이다.
 */
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
    Optional<UserStatistics> findByUser_Id(Long userId);  // 유저 정보로 유저 통계를 검색한다.
}
