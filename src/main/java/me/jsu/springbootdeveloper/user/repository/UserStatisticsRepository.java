// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    유저의 통계 정보를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 UserStatistics, 엔티티의 pk 타입은 Long이다.
 */
public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {
    Optional<UserStatistics> findByUser_Id(Long userId);  // 유저 정보로 유저 통계를 검색한다.

    // 유저 id로 통계를 배치 단위로 검색한다.
    @Query("""
        select us
        from UserStatistics us
        join fetch us.user u
        where us.id > :lastId
        order by us.id asc
    """)
    List<UserStatistics> findAllAfterId(
            @Param("lastId") Long lastId,
            Pageable pageable
    );

    /*
        유저 통계 테이블에서 user가 특정 유저인 모든 행에 대해 user_id 칼럼을 null로 바꾸는 쿼리문이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserStatistics us
        set us.user = null
        where us.user.id in :userIds
    """)
    void detachUser(@Param("userIds") List<Long> userIds);

    // 유저 id로 통계를 리스트로 통째로 조회하는 쿼리문이다. 탈퇴한 유저는 검색에서 제외한다.
    @Query("""
        select us
        from UserStatistics us
        join fetch us.user u
        where us.user.id in :userIds
    """)
    List<UserStatistics> findAllByUserIds(@Param("userIds") List<Long> userIds);

    /*
        탈퇴한 유저 (user = null -> inner join이므로)를 제외하고 모든 유저의 통계를 조회한다.
        유저 통계의 총 수행 일자의 정합성을 위해 조회한다.
     */
    @Query("""
        select us
        from UserStatistics us
        join fetch us.user
    """)
    List<UserStatistics> findAllForRepair();
}
