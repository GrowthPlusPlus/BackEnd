// 정성우

package me.jsu.springbootdeveloper.dto.article;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    유저가 서버에 올릴 게시글 정보를 담고 있는 클래스이다.
 */
@NoArgsConstructor  // 인자를 요구하지 않는 기본 생성자를 만들어주는 기능이다.
@AllArgsConstructor  // 모든 필드 값을 파라미터로 받는 생성자를 만들어주는 기능이다.
@Getter
public class AddArticleRequest {
    private String title;
    private String content;
    private Long challengeId;  // 어떤 챌린지에 속하는지 알도록 하는 필드이다.
}
