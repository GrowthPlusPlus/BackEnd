// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Builder;
import lombok.Getter;

/*
    유저의 챌린지 별 정보가 들어가있는 dto이다.
 */
@Builder
@Getter
public class MyChallenge {
    private Long challengeId;
    private String title;
    private int requiredWeeklyCount;  // 주간 최소 인증 횟수이다.
    private long dueToDate;  // 남은 챌린지 일수이다.
    private long currentStreak;  // 최근 연속으로 인증한 일 수이다.
    private int todaySuccessCount;  // 오늘 챌린지를 완수한 사람의 수이다.
    private int participantNumber;  // 챌린지의 총 참가자 수이다.
    private boolean doIt;  // 해당 챌린지를 오늘 수행했는지에 관한 불린 값이다.
    private boolean warning;  // 오늘 챌린지를 수행하지 않으면 실패하는지에 관한 불린 값이다.
}
