// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/*
    매일 유저의 챌린지 수행 여부를 체크하면서 단 하나의 챌린지도 수행하지 않은 경우 연속 일수를 제거하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class UserDailyStatisticsService {
    private final UserStatisticsRepository userStatisticsRepository;
    private final ChallengeQueryService challengeQueryService;

    private static final int BATCH_SIZE = 500;  // 배치 크기

    @Transactional
    public void settleDailyStatistics(LocalDate targetDate) {
        Long lastId = 0L;  // 0번 유저부터 모든 통계를 조사한다.
        List<UserStatistics> batch;

        do {
            // 유저 통계를 검색한다.
            batch = userStatisticsRepository.findAllAfterId(lastId, PageRequest.of(0, BATCH_SIZE));

            for (UserStatistics userStats : batch) {
                Long userId = userStats.getUser().getId();

                // 어제 하루 동안 인증 성공이 있었는지 검사한다.
                boolean wasActive = challengeQueryService.existsByUser_IdAndPostDateForLog(userId, targetDate);
                if (!wasActive && userStats.getCurrentStreakEndDate() != null) {  // 로그가 없다면 연속 일수를 초기화 한다.
                    userStats.recordDailyFail();
                }

                lastId = userStats.getId();
            }
        } while (!batch.isEmpty());
    }
}
