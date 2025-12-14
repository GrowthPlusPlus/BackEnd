package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;

@NoArgsConstructor  //기본 생성자
@AllArgsConstructor  //모든 필드 값을 파라미터로 받는 생성자 -> 위의 기본 생성자가 있어야 사용 가능함
@Getter
public class AddArticleRequest {
    private String title;
    private String content;

    public Article toEntity(String author) {  //맴버변수의 값을 데이터베이스에 넣을 준비를 함
        return Article.builder()
                .title(title)
                .content(content)
                .author(author)
                .build();
    }
}
