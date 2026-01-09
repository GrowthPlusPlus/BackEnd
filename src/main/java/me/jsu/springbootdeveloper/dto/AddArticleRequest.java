// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;

/*
    유저가 서버에 올릴 게시글 정보를 담고 있는 클래스이다.
 */
@NoArgsConstructor  // 인자를 요구하지 않는 기본 생성자를 만들어주는 기능이다.
@AllArgsConstructor  // 모든 필드 값을 파라미터로 받는 생성자를 만들어주는 기능이다.
@Getter
public class AddArticleRequest {
    private String title;
    private String content;

    public Article toEntity(String author) {  // DB에 넣을 게시글에 author 정보를 포함시켜 Article 객체를 생성한다.
        return Article.builder()
                .title(title)
                .content(content)
                .author(author)
                .build();
    }
}
