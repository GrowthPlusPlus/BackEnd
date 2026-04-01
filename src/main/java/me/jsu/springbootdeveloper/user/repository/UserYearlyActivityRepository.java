// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.UserYearlyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    유저의 연간 잔디를 관리하기 위한 리포지토리이다.
 */
public interface UserYearlyActivityRepository extends JpaRepository<UserYearlyActivity, Long> {
    Optional<UserYearlyActivity> findByUser_IdAndYear(Long userId, int year);  // 유저 아이디와 연도로 잔디 통계를 검색한다.

    // 유저 id 리스트로 한 번에 UserYearlyActivity를 조회하는 함수이다.
    @Query("""
        select uya
        from UserYearlyActivity uya
        join fetch uya.user u
        where u.id in :userIds
          and uya.year = :year
    """)
    List<UserYearlyActivity> findByUserIdsWithUser(
            @Param("userIds") List<Long> userIds,
            @Param("year") int year
    );
}