// 정성우

package me.jsu.springbootdeveloper.post.dto.verify;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    clip에 사진 검증 요청을 보내기 위해 사용하는 dto이다.
 */
@Getter
@NoArgsConstructor
public class ClipRequest {
    @NotNull
    private Long challengeId;  // 챌린지 id

    @NotNull
    private Long temporaryImageId;  // 검사할 임시 사진 id
}
