// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.domain.UserYearlyActivity;
import me.jsu.springbootdeveloper.user.dto.UserYearlyActivityResponse;
import me.jsu.springbootdeveloper.user.exception.InvalidYearException;
import me.jsu.springbootdeveloper.user.exception.UserStatisticsNotFoundException;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import me.jsu.springbootdeveloper.user.repository.UserYearlyActivityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/*
    유저의 통계를 관리하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class UserStatisticsService {
    private final UserStatisticsRepository userStatisticsRepository;
    private final UserYearlyActivityRepository userYearlyActivityRepository;
    private final ChallengeQueryService challengeQueryService;
    private final UserService userService;

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

    /*
        onTodayPostCancelled는 당일 전용이므로 쓰지 않고, 로그를 삭제한 뒤 남아 있는 ChallengeDailyLog 기준으로 유저 전역 통계를 다시 맞춘 뒤 대표 챌린지를 재선정한다.

        @param userId 통계를 갱신할 유저 id
     */
    @Transactional
    public void resyncUserStatisticsAfterPastPostDeletion(Long userId) {
        UserStatistics userStats = findByUser_IdStatistics(userId);  // 유저 통계를 검색한다.
        List<LocalDate> dates = challengeQueryService.findDistinctPostDatesByUserIdForChallenges(userId);  // 삭제된 로그 이후를 기준으로 유저의 모든 로그를 조회한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        userStats.rebuildGlobalStreakAndActivityFromDistinctDates(dates, today);
        recomputeDominant(userStats, userId);  // 대표 챌린지를 재선정한다.
    }

    /*
        연도를 기준으로 유저의 잔디를 조회한다.
        기존 잔디 값 + 오늘 데일리 로그 값으로 전체 잔디를 계산한다.
     */
    @Transactional(readOnly = true)
    public UserYearlyActivityResponse searchYearGrass(String email, int year) {
        User user = userService.findByEmail(email);

        if (year < 2026) {  // 해냄 서비스는 2026년 이후로 시작했으므로 이전의 잔디 값은 존재하지 않으므로 예외처리 한다.
            throw new InvalidYearException("올바르지 않은 연도입니다.");
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        int index = today.getDayOfYear() - 1;  // 오늘이 365일 또는 366일 중 몇 번째 인덱스에 해당하는지 구한다.

        // 오늘 유저가 수행한 총 인증글 개수를 조회한다.
        int count = challengeQueryService.countByPostDateWithChallenge(user.getId(), today);

        UserYearlyActivity userYearlyActivity = userYearlyActivityRepository.findByUser_IdAndYear(user.getId(), year)
                .orElse(null);

        if (userYearlyActivity == null) {  // 해당 연도에 인증글을 처음 올리고 하루도 지나지 않은 경우나, 이전에 올린적이 없는 경우이다.
            // 윤년에 따라 자동으로 365 또는 366길이의 배열을 임시로 생성한다.
            int length = Year.of(year).length();
            int[] tempGrass = new int[length];

            if (year == today.getYear()) {  // 조회하는 연도가 오늘과 같다면 오늘 수행한 총 로그 수를 반영한다.
                tempGrass[index] = count;
            }

            return UserYearlyActivityResponse.builder()
                    .activity(tempGrass)
                    .build();
        }
        else {
            int[] dayBitCount = Arrays.copyOf(userYearlyActivity.getDayBitCount(), userYearlyActivity.getDayBitCount().length);  // 기존 배열값을 카피한다.

            if (year == today.getYear()) {  // 조회하는 연도가 오늘과 같다면 오늘 수행한 총 로그 수를 반영한다.
                dayBitCount[index] = count;  // 기존 배열에 오늘 값을 추가한다. 단 엔티티의 값은 수정하지 않는다.
            }

            return UserYearlyActivityResponse.builder()
                    .activity(dayBitCount)
                    .build();
        }
    }

    /*
        관리자에 의해 과거 인증글이 삭제되어 로그도 삭제된 경우, 해당 유저의 로그를 모두 재조회하여 반영한다.
     */
    @Transactional
    public void researchLog(Long userId, LocalDate postDate) {
        List<Long> userIds = List.of(userId);
        LocalDate startDate = postDate.withDayOfYear(1);  // 검색할 연간의 시작일이다.
        LocalDate endDate = postDate.withDayOfYear(postDate.lengthOfYear());  // 검색할 연의 마지막날이다.

        // 해당 연도에 해당하는 모든 로그 개수를 수집한다.
        List<Object[]> yearlyLogs = challengeQueryService.countYearlyLogsByUsers(userIds, startDate, endDate);

        // 기존의 연간 활동(잔디)을 검색한다.
        UserYearlyActivity userYearlyActivity = userYearlyActivityRepository.findByUser_IdAndYear(userId, postDate.getYear())
                .orElse(null);

        // 오늘 한 인증 외, 이전에는 인증한 적이 없어서 테이블이 없는 경우 업데이트 할 필요가 없다.
        if (userYearlyActivity == null) {
            return;
        }

        // 삭제된 로그의 연도를 기준으로 배열을 새로 생성한다.
        int[] grass = new int[postDate.lengthOfYear()];

        // 로그를 배열에 반영한다. row[0]값은 모두 같은 유저이므로 사용하지 않는다.
        for (Object[] row : yearlyLogs) {
            LocalDate date = (LocalDate) row[1];  // 날짜를 추출한다.
            int count = ((Long) row[2]).intValue();  // 해당 날짜의 총 로그 개수를 추출한다.

            int index = date.getDayOfYear() - 1;  // 날짜 정보를 바탕으로 인덱스를 구한다.
            grass[index] = count;
        }

        // 기존 배열을 교체한다.
        userYearlyActivity.replaceDayBitCount(grass);
    }
}
