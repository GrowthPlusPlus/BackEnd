// 정성우

package me.jsu.springbootdeveloper.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    댓글을 생성하기 위해 필요한 정보를 가지고 있는 dto이다.
 */
@Getter
@NoArgsConstructor
public class CommentCreateRequest {
    @NotBlank
    @Size(max = 500)  // 최대 댓글 길이이다.
    private String contents;
}
