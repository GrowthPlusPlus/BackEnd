// 정성우

package me.jsu.springbootdeveloper.challenge.dto.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지 상세 정보를 반환하는 dto이다.
 */
@Getter
@AllArgsConstructor
public class ChallengeDetailResponse {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private int requiredWeeklyCount;
    private boolean photoRequired;

    private List<String> tags;
    private String description;

    private HostInfo host;
    private int participantCount;

    private List<UserSummary> todaySuccessUsers;

    @Getter
    @AllArgsConstructor
    public static class HostInfo {  // 방장 정보이다.
        private String name;
        private String profileImageUrl;
    }

    @Getter
    @AllArgsConstructor
    public static class UserSummary {  // 오늘 인증한 사람들의 정보이다.
        private String name;
        private String profileImageUrl;
    }
}