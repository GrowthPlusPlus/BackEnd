// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeCalenderShare;
import me.jsu.springbootdeveloper.challenge.enumType.ShareImageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/*
    챌린지 캘린더 공유를 위한 레포지토리이다.
 */
public interface ChallengeCalenderShareRepository extends JpaRepository<ChallengeCalenderShare, Long> {
    // 공유한 챌린지 캘린더 사진을 검색한다.
    Optional<ChallengeCalenderShare> findByUser_IdAndChallenge_IdAndYearAndMonth(Long userId, Long challengeId, int year, int month);

    // 오랜 시간이 지나서 만료됐거나, 재시도 횟수가 많은 이미지들을 검색한다. 삭제에 사용한다.
    @Query("""
        select csi
        from ChallengeCalenderShare csi
        where csi.expiredAt < :now
           or (csi.status = 'PROCESSING' and csi.retryCount >= 3)
        """)
    List<ChallengeCalenderShare> findImagesForCleanup(
            @Param("now") LocalDateTime now
    );

    // 일정 시간동안 처리되지 않거나, 실패했거나, 오랫동안 재시도한 비동기 작업들을 검색하는 쿼리문이다. 재시도에 사용한다.
    @Query("""
        select csi
        from ChallengeCalenderShare csi
        where csi.status = :status
          and csi.updateAt < :threshold
          and csi.retryCount < 3
    """)
    List<ChallengeCalenderShare> findStuckProcessingImages(
            @Param("status") ShareImageStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}
