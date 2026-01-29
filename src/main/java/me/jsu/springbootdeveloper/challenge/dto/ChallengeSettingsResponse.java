// 정성우

package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;

import java.time.LocalDate;
import java.util.List;

/*
    방장이 챌린지 설정을 하기 전에, 원래 적용되어있던 설정을 보내주는 dto이다.
 */
@Getter
@Builder
public class ChallengeSettingsResponse {
    private final String title;         // 화면에 보여주기용
    private final LocalDate startDate;  // 화면에 보여주기용

    private final int endDate;  // 저장 형식은 LocalDate지만, 사용자에게 보내줄땐 int로 변경한다.
    private final int requiredWeeklyCount;

    private final List<TagResponse> challengeTags;  // 현재 설정된 태그들이다.
    private final List<TagResponse> allTags;  // 유저가 선택하도록 모든 태그를 보낸다.

    private final String content;
    private final boolean photoRequired;
    private final ChallengeVisibility challengeVisibility;
}
