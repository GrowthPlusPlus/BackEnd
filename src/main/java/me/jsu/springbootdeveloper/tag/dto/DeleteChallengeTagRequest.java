// 정성우

package me.jsu.springbootdeveloper.tag.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    유저가 선택한 챌린지 태그를 삭제하는 dto이다.
 */
@Getter
@NoArgsConstructor
public class DeleteChallengeTagRequest {
    @NotNull
    private Long challengeId;
    @NotEmpty
    private List<Long> tagIds;
}
