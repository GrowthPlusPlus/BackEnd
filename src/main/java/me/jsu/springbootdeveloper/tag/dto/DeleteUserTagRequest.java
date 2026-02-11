// 정성우

package me.jsu.springbootdeveloper.tag.dto;
import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    유저에 저장된 태그를 삭제하는 dto이다.
 */
@Getter
@NoArgsConstructor
public class DeleteUserTagRequest {
    @NotEmpty
    private List<Long> tagIds;
}