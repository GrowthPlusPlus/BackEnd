package me.jsu.springbootdeveloper.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.dto.ArticleListViewResponse;
import me.jsu.springbootdeveloper.dto.ArticleViewResponse;
import me.jsu.springbootdeveloper.service.BlogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor  //final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 함
@Controller  //html로 전달
public class BlogViewController {

    private final BlogService blogService;

    @GetMapping("/articles")
    public String getArticles(Model model) {
        List<ArticleListViewResponse> articles = blogService.findAll().stream()
                .map(ArticleListViewResponse::new)
                .toList();
        model.addAttribute("articles", articles);  //블로그에 보여줄 글 리스트 저장

        return "articleList";
    }

    @GetMapping("/articles/{id}")
    public String getArticle(@PathVariable Long id, Model model) {
        Article article = blogService.findById(id);
        model.addAttribute("article", new ArticleViewResponse(article));

        return "article";
    }

    /*
    model 객체 덕분에 model에 add로 값을 넣어두면 return한 html에서 (add로 넣은 객체의 이름).(객체 내용)으로 사용 가능함
     */

    @GetMapping("/new-article")
    public String newArticle(@RequestParam(required = false) Long id, Model model) {  //@RequestParam 덕분에 파라미터가 없어도 오류 없이 실행됨
        if (id == null) {  //들어온 id가 없으면 수정이 아닌 생성
            model.addAttribute("article", new ArticleViewResponse());
        }
        else {  // id가 존재하므로 해당 아이디에 대한 article을 찾고 수정
            Article article = blogService.findById(id);
            model.addAttribute("article", new ArticleViewResponse(article));
        }

        return "newArticle";
    }
}
