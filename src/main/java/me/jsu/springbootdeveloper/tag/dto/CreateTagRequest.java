package me.jsu.springbootdeveloper.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.tag.domain.TagCategory;

/*
    새로운 태그를 생성할 때 사용하는 요청 DTO이다.
 */
@Getter
@NoArgsConstructor
public class CreateTagRequest {

    @NotBlank(message = "태그 이름을 입력해주세요.")
    private String tagName;

    @NotNull(message = "태그 카테고리를 선택해주세요.")
    private TagCategory tagCategory;
}
