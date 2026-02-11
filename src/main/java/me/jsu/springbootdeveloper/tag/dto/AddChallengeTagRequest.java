// 정성우

package me.jsu.springbootdeveloper.tag.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/*
    유저가 선택한 태그를 챌린지와 연결하는 dto이다.
 */
@Getter
@NoArgsConstructor
public class AddChallengeTagRequest {
    @NotNull
    private Long challengeId;
    private List<Long> tagIds;
}
