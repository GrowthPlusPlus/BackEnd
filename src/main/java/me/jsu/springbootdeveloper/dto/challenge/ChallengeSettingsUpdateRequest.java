// 정성우

package me.jsu.springbootdeveloper.dto.challenge;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import jakarta.validation.constraints.Min;

import java.util.List;

/*
    챌린지 설정 변경사항을 담고있는 dto이다. 클라이언트는 해당 정보를 서버에 보낸다.
 */
@Getter
@Builder
public class ChallengeSettingsUpdateRequest {
    @Min(1)  // 비어있음, 0, 음수를 금지한다.
    private final int endDate;  // 인증 기간이다. 서버에서 LocalDate로 변경한다.
    @Min(1)
    private final int requiredWeeklyCount; // 주간 인증 빈도

    private final List<Long> tagIds;  // 챌린지 태그
    private final String content;  // 챌린지 설명

    @NotNull
    private final Boolean photoRequired;  // 사진 첨부 필수 여부
    @NotNull
    private final ChallengeVisibility challengeVisibility;  // 공개 범위
}
