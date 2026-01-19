// 정성우

package me.jsu.springbootdeveloper.repository.user;

import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.domain.user.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/*
    유저의 통계 정보를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 UserStatistics, 엔티티의 pk 타입은 Long이다.
 */
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
    Optional<UserStatistics> findByUser_Id(Long userId);  // 유저 정보로 유저 통계를 검색한다.

    /*
        유저 통계 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserStatistics us
        set us.user = null
        where us.user = :user
    """)
    void detachUser(@Param("user") User user);
}
