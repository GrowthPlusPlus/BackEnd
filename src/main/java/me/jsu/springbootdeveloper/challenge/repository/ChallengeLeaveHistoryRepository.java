// 정성우

package me.jsu.springbootdeveloper.challenge.repository;

import me.jsu.springbootdeveloper.challenge.domain.ChallengeLeaveHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    챌린지를 나가거나 강퇴된 이력을 조회하는 저장소이다.
 */
public interface ChallengeLeaveHistoryRepository extends JpaRepository<ChallengeLeaveHistory, Long> {

    /*
        사용자가 해당 챌린지를 나간 적이 있는지 확인한다.
        스스로 나간 경우와 강퇴된 경우를 구분하지 않는다.

        @param userId 사용자 id
        @param challengeId 챌린지 id
        @return 재참여를 막아야 하면 true
     */
    boolean existsByUser_IdAndChallenge_Id(Long userId, Long challengeId);

    /*
        검색 결과에 들어 있는 챌린지 중, 사용자가 이미 떠났던 챌린지 id만 고른다.

        @param userId 현재 사용자 id
        @param challengeIds 이번 검색 페이지의 챌린지 id 목록
        @return joinedBefore를 true로 표시할 챌린지 id 목록
     */
    @Query("""
            select history.challenge.id
            from ChallengeLeaveHistory history
            where history.user.id = :userId
              and history.challenge.id in :challengeIds
            """)
    List<Long> findChallengeIdsByUserIdAndChallengeIds(
            @Param("userId") Long userId,
            @Param("challengeIds") List<Long> challengeIds
    );
}
