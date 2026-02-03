// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Getter;

/*
    챌린저 별 캘린더의 맨 위 상단의 정보를 포함하는 dto이다.
 */
@Getter
public class ChallengeCalendarResponse {
    private final String title;  // 챌린지 이름
    private final int totalSuccessDays;     // 완료 일수
    private final int currentStreakDays;    // 연속 일수
    private final boolean challengeOwner;  // 해당 챌린지에 대한 방장 여부

    public ChallengeCalendarResponse(String title, int totalSuccessDays, int currentStreakDays, boolean challengeOwner) {
        this.title = title;
        this.totalSuccessDays = totalSuccessDays;
        this.currentStreakDays = currentStreakDays;
        this.challengeOwner = challengeOwner;
    }
}