// 정성우

package me.jsu.springbootdeveloper.post.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    유저가 서버에 올릴 게시글 정보를 담고 있는 클래스이다.
 */
@NoArgsConstructor  // 인자를 요구하지 않는 기본 생성자를 만들어주는 기능이다.
@AllArgsConstructor
@Getter
public class AddArticleRequest {
    @NotBlank
    @Size(max = 500)  // 최대 댓글 길이이다.
    private String content;
    private Long challengeId;  // 어떤 챌린지에 속하는지 알도록 하는 필드이다.
    private List<Long> tempImageIds;
}
