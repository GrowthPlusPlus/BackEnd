// 정성우

package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.dto.DailyGraphResponse;
import me.jsu.springbootdeveloper.user.dto.WeeklyGraphResponse;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
    유저의 월별, 주간별 챌린지 성공 추이를 기록하고 보여주는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class UserProgressStatisticsService {
    private final UserService userService;
    private final ChallengeQueryService challengeQueryService;

    /*
        월간 추이를 반환하는 함수로, 주 4개로 계산한다.
     */
    public WeeklyGraphResponse getRecent4WeeksGraph(String email) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 이번 달 시간이다.
        LocalDate thisEnd = today.with(DayOfWeek.MONDAY);  // 오늘을 기준으로 이번 주의 시작일이다.
        LocalDate thisStart = thisEnd.minusWeeks(3);  // 이번 주의 시작일을 기준으로 3주 전의 주의 시작일을 구한다. 따라서 딱 4주간의 데이터를 얻을 수 있다.

        // 저번 달 시간이다.
        LocalDate lastMonthDate = today.minusMonths(1);  // 오늘을 기준으로 저번 달을 구한다.
        LocalDate lastEnd = lastMonthDate.with(DayOfWeek.MONDAY);
        LocalDate lastStart = lastEnd.minusWeeks(3);

        // 이번 달을 기준으로 총 4주간의 챌린지 성공 횟수를 구한다.
        List<Integer> thisMonth = extractWeeklyCounts(
                challengeQueryService.sumWeeklyInRange(user.getId(), thisStart, thisEnd),  // 일정 기간 동안의 챌린지 성공 횟수이다. [0]: 주의 시작일, [1]: 해당 주의 성공 횟수
                thisStart  // [0]에 있는 값과 매칭하기 위한 매개변수이다.
        );

        // 저번 달을 기준으로 총 4주간의 챌린지 성공 횟수를 구한다.
        List<Integer> lastMonth = extractWeeklyCounts(
                challengeQueryService.sumWeeklyInRange(user.getId(), lastStart, lastEnd),
                lastStart
        );

        return WeeklyGraphResponse.builder()
                .thisMonth(thisMonth)
                .lastMonth(lastMonth)
                .build();
    }

    /*
        검색한 주간 값이 연속으로 4주라는 보장이 없다는 점, 중간 주에 한 번도 수행하지 않으면 주 개수가 부족할 수 있는 점을 보안하기 위하여
        해당 부분들을 채워주는 역할을 하는 함수이다. 즉 보정의 역할을 한다.
     */
    private List<Integer> extractWeeklyCounts(List<Object[]> rows, LocalDate start) {
        Map<LocalDate, Integer> map = new HashMap<>();  // 빠른 조회를 위해 날짜와 해당 날짜의 성공 횟수에 대한 map을 생성한다.

        for (Object[] row : rows) {
            LocalDate week = (LocalDate) row[0];  // 해당 주의 시작일이다.
            int count = ((Number) row[1]).intValue();  // 해당 주의 성공 횟수이다.
            map.put(week, count);  // 둘을 key-value로 매핑한다.
        }

        List<Integer> result = new ArrayList<>();  // 최종적으로 컨트롤러에 반환할 리스트이다.
        LocalDate cursor = start;  // 반복의 기준이 되는 날짜를 start로 잡는다. 여기선 첫 번째 주의 기준 날짜다.
        for (int i = 0; i < 4; i++) {  // 4번 반복하여 4주치 데이터를 만든다. 따라서 비어있는 주도 생성이 가능하다.
            LocalDate weekStart = cursor.with(java.time.DayOfWeek.MONDAY);  // 주에 해당하는 월요일 날짜를 구한다. (DayOfWeek.MONDAY는 방어 코드)
            result.add(map.getOrDefault(weekStart, 0));  // map에서 weekStart에 맞는 성공 횟수를 꺼내고, 없다면 0을 넣는다.
            cursor = cursor.plusWeeks(1);  // 주차를 1주 뒤로 이동시킨다.
        }

        return result;
    }

    /*
        주간 추이를 반환하는 함수로, 총 7일로 계산한다.
     */
    public DailyGraphResponse getWeeklyGraph(String email) {
        User user = userService.findByEmail(email);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 이번 주 시간이다.
        LocalDate thisWeekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);

        // 저번 주 시간이다.
        LocalDate lastWeekStart = thisWeekStart.minusWeeks(1);
        LocalDate lastWeekEnd = lastWeekStart.plusDays(6);

        // 이번 주를 기준으로 총 7일간의 챌린지 성공 횟수를 구한다.
        List<Integer> thisWeek = extractDailyCounts(
                challengeQueryService.countDailyRange(user.getId(), thisWeekStart, thisWeekEnd),  // 날짜 사이의 성공 횟수를 구한다. (날짜-성공횟수)
                thisWeekStart  // [0]에 있는 값과 매칭하기 위한 매개변수이다.
        );

        // 저번 주를 기준으로 총 7일간의 챌린지 성공 횟수를 구한다.
        List<Integer> lastWeek = extractDailyCounts(
                challengeQueryService.countDailyRange(user.getId(), lastWeekStart, lastWeekEnd),
                lastWeekStart
        );

        return DailyGraphResponse.builder()
                .thisWeek(thisWeek)
                .lastWeek(lastWeek)
                .build();
    }

    /*
        구한 7일간의 값을 보정하는 역할을 한다.
     */
    private List<Integer> extractDailyCounts(List<Object[]> rows, LocalDate start) {
        Map<LocalDate, Integer> map = new HashMap<>();  // 빠른 검색을 위해 map을 생성한다.

        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            int count = ((Number) row[1]).intValue();
            map.put(date, count);
        }

        List<Integer> result = new ArrayList<>();  // 컨트롤러에 반환할 리스트이다.
        for (int i = 0; i < 7; i++) {  // 7일치 데이터를 생성한다. 따라서 빈 값도 생성할 수 있다.
            LocalDate date = start.plusDays(i);
            result.add(map.getOrDefault(date, 0));  // start 날부터 하나씩 늘려가며 성공 횟수를 구한다.
        }

        return result;
    }
}
