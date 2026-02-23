// 정성우

package me.jsu.springbootdeveloper.challenge.dto.preview;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    사진으로 검증하기에 적합한지 알고싶은 챌린지 이름이 들어있는 dto이다.
 */
@Getter
@NoArgsConstructor
public class ChallengePreviewRequest {
    @NotBlank
    private String title;
}
