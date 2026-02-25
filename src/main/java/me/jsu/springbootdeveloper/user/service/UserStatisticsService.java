// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.exception.UserStatisticsNotFoundException;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/*
    유저의 통계를 관리하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class UserStatisticsService {
    private final UserStatisticsRepository userStatisticsRepository;
    private final ChallengeQueryService challengeQueryService;

    /*
       챌린지를 인증하여 로그가 확정됐을 때 수행하는 함수이다.
       유저 통계, 대표 챌린지를 갱신한다.
     */
    @Transactional
    public void onPostConfirmed(User user, Long challengeId, LocalDate today) {
        // 유저 통계를 가져온다. 없다면 생성한다.
        UserStatistics userStats = userStatisticsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> userStatisticsRepository.save(UserStatistics.builder()
                        .user(user)
                        .build()));

        // 챌린지 참가 정보를 가져온다.
        ChallengeParticipant participant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        // 유저 전체 활동일수(오늘 활동 처리)
        userStats.markActiveToday(today);  // 유저 최근 활동 일수에서, 오늘 활동을 했음을 반영한다.
        userStats.recordDailySuccess(today);  // 유저의 연속 성공 일수를 갱신한다.

        // 대표 챌린지 (dominant)후보와 비교한다.
        userStats.considerDominantChallenge(challengeId, participant.getTotalSuccessDays());
    }

    /*
        당일 인증글을 삭제하는 경우 반영한다. 과거의 인증글은 삭제 불가능하도록 해야 한다.
        오늘 날짜에 유저가 다른 챌린지 인증도 했는지 확인 후, 없으면 UserStatistics에서 오늘 활동을 롤백한다. 즉 로그를 검사한다.
        대표 챌린지가 영향을 받으면 재선정 한다. (전체 참가 챌린지 스캔)
     */
    @Transactional
    public void onTodayPostCancelled(Long userId, Long challengeId, LocalDate today) {
        // 유저 통계를 검색한다.
        UserStatistics userStats = findByUser_IdStatistics(userId);

        // 오늘 날짜에 유저가 수행한 다른 인증 로그가 남아있는지 확인한다.
        boolean stillActiveToday = challengeQueryService.existsByUser_IdAndPostDateForLog(userId, today);

        // 오늘 활동이 완전히 0이 되었다면, 유저 활동일수/연속일수를 롤백한다.
        if (!stillActiveToday) {
            userStats.unmarkActiveToday(today);  // 최근 활동 일수를 롤백한다.
            userStats.removeDailySuccess(today);  // 연속 기록을 롤백한다.
        }

        // 대표 챌린지가 취소된 챌린지라면 대표를 재선정한다.
        if (userStats.getDominantChallengeId() != null && userStats.getDominantChallengeId().equals(challengeId)) {
            recomputeDominant(userStats, userId);
        }
    }

    /*
       대표 챌린지 (dominant)를 재선정한다.
       당일 취소로 score가 감소하면 2등이 누구였는지 모르므로 유저가 참여한 챌린지의 전체 스캔이 필요하다.
     */
    private void recomputeDominant(UserStatistics userStats, Long userId) {
        List<ChallengeParticipant> participants = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(userId, Status.IN_PROGRESS);

        Long bestId = null;
        int bestScore = 0;

        // 제일 스코어가 높은 챌린지를 대표 챌린지로 설정한다.
        for (ChallengeParticipant p : participants) {
            int score = p.getTotalSuccessDays();
            if (bestId == null || score > bestScore) {
                bestId = p.getChallenge().getId();
                bestScore = score;
            }
        }

        // 재선정 결과를 DB에 반영한다.
        userStats.setDominantChallenge(bestId, bestScore);
    }

    /*
        챌린지가 완료된 경우, 유저가 성공한 챌린지 개수를 업데이트 한다.
     */
    public void updateCompletedChallengeCount(Long userId) {
        UserStatistics userStatistics = findByUser_IdStatistics(userId);
        userStatistics.increaseCompletedChallengeCount();
    }

    /*
        챌린지를 실패한 경우, 유저가 실패한 챌린지 개수를 업데이트 한다.
     */
    public void updateFailedChallengeCount(Long userId) {
        UserStatistics userStatistics = findByUser_IdStatistics(userId);
        userStatistics.increaseFailedChallengeCount();
    }

    /*
        유저 통계를 검색하는 함수이다.
     */
    public UserStatistics findByUser_IdStatistics(Long userId) {
        return userStatisticsRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserStatisticsNotFoundException("UserStatistics가 없습니다."));
    }
}
