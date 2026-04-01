// 정성우

package me.jsu.springbootdeveloper.user.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.domain.UserYearlyActivity;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import me.jsu.springbootdeveloper.user.repository.UserYearlyActivityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    매일 밤마다 유저 통계에 관련한 서비스를 처리하는 클래스이다.
 */
@RequiredArgsConstructor
@Service
public class UserDailyStatisticsService {
    @PersistenceContext
    private EntityManager entityManager;

    private final UserStatisticsRepository userStatisticsRepository;
    private final UserYearlyActivityRepository userYearlyActivityRepository;
    private final ChallengeQueryService challengeQueryService;

    private static final int BATCH_SIZE = 500;  // 배치 크기

    /*
        매일 유저의 챌린지 수행 여부를 체크하면서 단 하나의 챌린지도 수행하지 않은 경우 연속 일수를 제거하는 함수이다.
     */
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

    /*
        매일 유저의 인증글 수행 여부를 체크하면서 잔디를 업데이트하는 함수이다. 로그가 존재하는데 잔디 db값이 없다면 생성한다.
     */
    @Transactional
    public void updateYearlyActivity(LocalDate today) {
        int index = today.getDayOfYear() - 1;  // 오늘이 365일 또는 366일 중 몇 번째 인덱스에 해당하는지 구한다.
        Long lastId = 0L;
        List<Object[]> batch;

        while (true) {
            // 오늘의 데일리 로그를 검색한다.
            batch = challengeQueryService.findUserDailyLogCountsBatch(today, lastId, PageRequest.of(0, BATCH_SIZE));

            if (batch.isEmpty()) {  // 배치가 끝나면 탈출한다.
                break;
            }

            // userIds를 추출한다.
            List<Long> userIds = batch.stream()
                    .map(row -> (Long) row[0])
                    .toList();

            // 기존 YearlyActivity를 조회한다.
            List<UserYearlyActivity> activities = userYearlyActivityRepository.findByUserIdsWithUser(userIds, today.getYear());

            // db에서 조회한 UserYearlyActivity를 빠르게 조회하기 위해 추출한 userIds와 매핑한다.
            Map<Long, UserYearlyActivity> activityMap =
                    activities.stream()
                            .collect(Collectors.toMap(
                                    a -> a.getUser().getId(),
                                    a -> a
                            ));

            /*
                위에서 조회한 UserYearlyActivity값이 유저 id와 매핑되지 않는 경우 처음으로 잔디 db 값을 생성해야 하므로 모든 로그를 조사해야 한다.
                따라서 db값이 없는 유저들의 id 리스트를 따로 생성하여 전체 로그 조사에 용이하게 한다.
             */
            List<Long> missingUserIds = userIds.stream()
                    .filter(id -> !activityMap.containsKey(id))
                    .toList();

            LocalDate startDate = today.withDayOfYear(1);  // 검색할 연간의 시작일이다.
            LocalDate endDate = today.withDayOfYear(today.lengthOfYear());  // 검색할 연의 마지막날이다.

            List<Object[]> yearlyLogs;
            if (missingUserIds.isEmpty()) {  // 모든 유저가 UserYearlyActivity를 가지고 있는 경우 빈 리스트를 넣는다.
                yearlyLogs = List.of();
            }
            else {
                // 없는 유저만 연간 전체 로그를 조회한다. Object의 인덱스마다 유저 id, 날짜, 해당 날짜에 유저가 생성한 인증글 수를 가지고 있다.
                yearlyLogs = challengeQueryService.countYearlyLogsByUsers(missingUserIds, startDate, endDate);
            }

            // userId를 날짜, 총 인증 횟수에 매핑한다. (맵 구조: user id -> 날짜 -> 날짜에 생성한 인증글 개수)
            Map<Long, Map<LocalDate, Integer>> yearlyMap = new HashMap<>();
            for (Object[] row : yearlyLogs) {
                Long uId = (Long) row[0];  // 유저 아이디를 뽑아낸다.
                LocalDate date = (LocalDate) row[1];  // 수행한 날짜를 뽑아낸다.
                int c = ((Long) row[2]).intValue();  // 수행한 날짜의 총 인증 횟수를 뽑아낸다.

                yearlyMap
                        .computeIfAbsent(uId, k -> new HashMap<>())  // 유저 id가 없으면 새 map을 생성하고, 있다면 기존 map을 가져온다.
                        .put(date, c);  // 해당하는 유저 id map에 날짜, 해당 날짜에 생성한 인증글 개수를 넣는다.
            }

            // group by와 orderBy를 통해 maxId를 정하지만, db 순서 결과에 문제가 생길 수 있으므로 배치 내에서 가장 id가 높은 값을 따로 구한다.
            Long batchLastId = batch.stream()
                    .map(row -> (Long) row[2])
                    .max(Long::compare)
                    .orElse(lastId);

            // 배치마다 잔디 값을 업데이트하거나 생성한다.
            for (Object[] row : batch) {
                Long userId = (Long) row[0];
                int count = ((Long) row[1]).intValue();  // 오늘 인증한 횟수이다.
                Long maxId = (Long) row[2];

                UserYearlyActivity activity = activityMap.get(userId);  // 이번 배치에 해당하는 유저의 잔디 db값을 가져온다.

                if (activity != null) {  // db값이 존재하면 기존 값에 업데이트한다.
                    activity.updateDayBitCount(index, count);
                }
                else {  // 기존 db값이 존재하지 않는다면, yearlyMap을 바탕으로 업데이트 한다.
                    // 이번 배치에 해당하는 유저의 id와 맞는 map 값을 yearlyMap에서 가져온다. 없다면 빈 맵을 대입한다.
                    Map<LocalDate, Integer> userLogs = yearlyMap.getOrDefault(userId, Map.of());

                    // 윤년에 따라 자동으로 365 또는 366길이의 배열을 생성한다.
                    int[] grass = new int[today.lengthOfYear()];

                    // 연간 로그를 배열로 변환한다.
                    for (Map.Entry<LocalDate, Integer> entry : userLogs.entrySet()) {  // 해당 유저의 map의 key와 value값을 한 번에 꺼낸다.
                        int idx = entry.getKey().getDayOfYear() - 1;  // key에 해당하는 LocalDate값의 연도를 기준으로 인덱스를 구한다.
                        grass[idx] = entry.getValue();  // 인덱스에 value값에 해당하는 Integer값을 넣는다.
                    }

                    // 오늘 값을 배열에 반영한다.
                    grass[index] = count;

                    // 유저의 객체를 조회하지 않고 UserYearlyActivity 객체를 생성하기 위해 사용한다.
                    User userRef = entityManager.getReference(User.class, userId);

                    // 엔티티 생성하여 db에 저장한다.
                    UserYearlyActivity newActivity = UserYearlyActivity.builder()
                            .user(userRef)
                            .year(today.getYear())
                            .dayBitCount(grass)
                            .build();

                    userYearlyActivityRepository.save(newActivity);
                }

                batchLastId = maxId;  // 임시로 저장한다.
            }

            lastId = batchLastId;  // 실제로 커서가 이동한다.
        }
    }
}
