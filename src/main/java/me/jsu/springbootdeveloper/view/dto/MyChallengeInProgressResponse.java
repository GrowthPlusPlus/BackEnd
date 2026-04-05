// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.enumType.Status;

import java.time.LocalDate;

/*
    나의 챌린지 중 현재 진행중인 챌린지 정보를 저장하는 dto이다.
 */
@Getter
@Builder
public class MyChallengeInProgressResponse {
    private Long challengeId;
    private String title;
    private int requiredWeeklyCount;
    private int todaySuccessCount;  // 오늘 챌린지를 완료한 사람 수이다.
    private int participantNumber;
    private long duringDate;  // 챌린지 경과일이다.
    private LocalDate endDate;
    private int achievementRate;
    private Status status;
    private long dueToDate;  // 남은 챌린지 일수이다.
    private long currentStreak;  // 최근 연속으로 인증한 일 수이다.
    private boolean doIt;  // 해당 챌린지를 오늘 수행했는지에 관한 불린 값이다.
}