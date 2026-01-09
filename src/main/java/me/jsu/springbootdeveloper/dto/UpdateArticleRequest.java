// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    게시글 업데이트를 위해 Article에서 수정된 부분을 가지고 있는 클래스이다.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UpdateArticleRequest {
    private String title;
    private String content;
}
