package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;

//데이터베이스 값을 조회하는 클래스
//데이터베이스의 Article 내용을 그대로 뽑아 쓰면 너무 많은 내용이 노출되므로 보여주고 싶은 내용만 뽑아서 사용자에게 보여주기 위해 사용한다
@Getter
public class ArticleResponse {

    private final String title;
    private final String content;

    public ArticleResponse(Article article) {
        this.title = article.getTitle();
        this.content = article.getContent();
    }
}
