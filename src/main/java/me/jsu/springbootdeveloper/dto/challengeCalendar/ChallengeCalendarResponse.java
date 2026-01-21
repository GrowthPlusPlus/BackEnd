// 정성우

package me.jsu.springbootdeveloper.dto.challengeCalendar;

import lombok.Getter;

/*
    챌린저 별 캘린더의 맨 위 상단의 정보를 포함하는 dto이다.
 */
@Getter
public class ChallengeCalendarResponse {
    private final int totalSuccessDays;     // 완료 일수
    private final int currentStreakDays;    // 연속 일수
    private final boolean challengeOwner;  // 해당 챌린지에 대한 방장 여부

    public ChallengeCalendarResponse(int totalSuccessDays, int currentStreakDays, boolean challengeOwner) {
        this.totalSuccessDays = totalSuccessDays;
        this.currentStreakDays = currentStreakDays;
        this.challengeOwner = challengeOwner;
    }
}