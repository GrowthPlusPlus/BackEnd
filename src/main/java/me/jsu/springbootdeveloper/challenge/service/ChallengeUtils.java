// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;

/*
    서비스와는 관련이 없는 유틸과 관련된 함수가 포함된 클래스이다.
 */
public final class ChallengeUtils {
    /*
       부분 주 보정 로직이다.
       주는 항상 월요일을 기준으로 시작하므로, 해당 유저가 주에 늦게 시작하여 불이익을 보거나 챌린지 마지막 주에 불이익을 보지 않도록 한다.
       주간 최소 인증 횟수를 비례 조정한다.
     */
    public static int calculateAdjustedRequired(Challenge challenge, LocalDate weekStart, LocalDate weekEnd) {
        // 챌린지 시작일과 주 시작일 중 더 늦은 날짜를 사용한다. 따라서 챌린지가 없던 날은 계산에서 제외한다.
        LocalDate effectiveStart;
        if (challenge.getStartDate().isAfter(weekStart)) {  // 챌린지 시작일이 주 시작일보다 더 늦은 경우이다.
            effectiveStart = challenge.getStartDate();  // 주의 시작인 월요일부터가 아닌, 챌린지 시작일부터 계산한다.
        }
        else {  // 일반적인 케이스이다.
            effectiveStart = weekStart;
        }

        // 챌린지 종료일과 주의 종료일 중 더 이른 날짜를 사용한다. 주는 일요일까지지만 챌린지가 금요일에 끝나는 경우 등을 고려한다.
        LocalDate effectiveEnd;
        if (challenge.getEndDate().isBefore(weekEnd)) {  // 챌린지 종료일이 주 종료일보다 빠른 경우이다.
            effectiveEnd = challenge.getEndDate();  // 주의 마지막인 일요일이 아닌, 챌린지 종료일로 계산한다.
        }
        else {  // 일반적인 경우이다.
            effectiveEnd = weekEnd;
        }

        // 이번 주에 챌린지가 실제로 존재한 날짜 수를 계산한다. 날짜의 양 끝을 포함하기 위해 +1을 한다.
        long daysInWeek = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;

        // 원래 주간 목표 * (실제 수행가능한 일수 / 7일) 값을 반올림 하여 최소 인증 횟수를 조정한다.
        return (int) Math.ceil(challenge.getRequiredWeeklyCount() * (daysInWeek / 7.0));
    }

    /*
        두 날짜 사이에 걸쳐 있는 캘린더 주차의 개수를 계산하는 함수이다.
        주간 챌린지 상태를 업데이트 할 때 사용하는 함수로, 해당 주차가 몇 주차인지 계산한다.
     */
    public static int calculateWeekIndex(LocalDate start, LocalDate end) {
        WeekFields wf = WeekFields.ISO;  // ISO 주차 규칙을 사용하는 기준점이다.

        LocalDate cursor = start.with(wf.dayOfWeek(), 1);   // 해당 날짜의 시작 주에서 월요일을 기준으로 한다.
        LocalDate endWeek = end.with(wf.dayOfWeek(), 1);  // 위와 같은 일을 한다. 따라서 end가 화요일이면 월요일로 시간을 바꿔주는 역할을 한다.

        int weeks = 1;
        while (cursor.isBefore(endWeek)) {  // 몇 주가 차이나는지 주 단위로 이동하며 계산한다.
            cursor = cursor.plusWeeks(1);
            weeks++;
        }

        return weeks;
    }

    /*
        D-day 날짜를 계산해주는 함수이다.

        기준:
        targetDate 기준으로 계산
        종료일 포함 (inclusive, +1)
        과거일 경우 0으로 보정

        @param targetDate 기준 날짜 (ex. 오늘, 특정 조회 날짜)
        @param endDate 챌린지 종료일
        @return 남은 일수 (0 이상)
     */
    public static long calculateDDay(LocalDate targetDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(targetDate, endDate) + 1;  // 종료일로부터 며칠 남았는지 일수로 계산한다.
        return Math.max(0, days);  // day 값이 음수가 나오는 것을 방지한다.
    }
}
