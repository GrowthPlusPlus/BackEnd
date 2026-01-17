// 정성우

package me.jsu.springbootdeveloper.dto.article;

import lombok.Getter;
import me.jsu.springbootdeveloper.domain.post.Article;

/*
    DB에 조회한 게시글의 정보를 가지고 있는 클래스이다.
    유저에게 게시글 정보를 반환하기 위해 사용한다.
 */
@Getter
public class ArticleResponse {
    private final String title;
    private final String content;

    public ArticleResponse(Article article) {
        this.title = article.getTitle();
        this.content = article.getContent();
    }
}
