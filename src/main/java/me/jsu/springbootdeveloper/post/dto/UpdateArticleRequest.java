// 정성우

package me.jsu.springbootdeveloper.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    게시글 업데이트를 위해 Article에서 수정된 부분을 가지고 있는 클래스이다.
    기존의 사진 삭제, 사진 추가를 할 수 있고 내용을 수정할 수 있다.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateArticleRequest {
    @NotBlank
    private String content;   // 수정할 인증글 내용
    @NotNull
    private List<Long> deleteImageIds;  // 삭제할 이미지 id 목록으로, 빈 리스트를 허용한다.
}
