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

    /*
        모든 유저의 연간 잔디를 모두 조회한다. (탈퇴한 유저는 제외) ex) 한 유저의 2026, 2027, 2028 모두 다 조회
        유저 통계의 수행 일수 정합성을 위해 사용한다.
     */
    @Query("""
        select uya
        from UserYearlyActivity uya
        join fetch uya.user
    """)
    List<UserYearlyActivity> findAllUserYearlyActivityForRepair();
}