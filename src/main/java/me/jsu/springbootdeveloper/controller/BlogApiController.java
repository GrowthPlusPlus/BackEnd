package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.dto.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.ArticleResponse;
import me.jsu.springbootdeveloper.dto.UpdateArticleRequest;
import me.jsu.springbootdeveloper.service.BlogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController  //http response body에 객체 데이터를 JSON형식으로 반환해주는 컨트롤러
public class BlogApiController {

    private final BlogService blogService;  //의존성 주입 -> save 사용 가능

    //http 메서드가 post이고 받은 url과 같으면 매핑함
    @PostMapping("/api/articles")  // post이므로 생성하겠다는 의미
    // @RequestBody로 요청 본문 값인 JSON형태를 AddArticleRequest의 맴버변수에 매핑함
    // 여기서 <Article>은 클라이언트에게 전달한 값의 형태임
    @ResponseBody  //@RestController가 있으므로 생략 가능
    public ResponseEntity<Article> addArticle(@RequestBody AddArticleRequest request, Principal principal) {  //AddArticleRequest 맴버변수에 맞지 않는 request값이 들어오면 무시함
        Article savedArticle = blogService.save(request, principal.getName());
        //ArticleResponse response = new ArticleResponse(savedArticle); 저장한 Article을 그대로 다시 반환하는 것은 위험하므로 정보를 정제함

        //요청이 성공적으로 작동하면 블로그 글 정보를 응답 객체에 담아 JSON으로 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)  //생성하는 함수이므로 CREATED로 설정
                .body(savedArticle);         //저장한 엔티티 값 실제로 반환
    }

    //전체 글을 조회 한 뒤에 반환해주는 매핑 클래스
    @GetMapping("/api/articles")
    public ResponseEntity<List<ArticleResponse>> findAllArticles() {
        List<ArticleResponse> articles = blogService.findAll()
                .stream()  //Stream<Article>로 변환
                .map(ArticleResponse::new)  //article을 ArticleResponse로 변환해줌
                .toList();  //Stream<ArticleResponse>를 List<ArticleResponse>로 변환

        return ResponseEntity.ok()
                .body(articles);  //성공적이면 findAll해서 얻은 정보 리턴
    }

    //아이디로 블로그 글을 조회하는 매핑 클래스
    @GetMapping("/api/articles/{id}")  //url 경로에서 id값 추출
    public ResponseEntity<ArticleResponse> findArticle(@PathVariable Long id) {  //검색할 id가 {id}로 들어옴
        Article article = blogService.findById(id);

        return ResponseEntity.ok()
                .body(new ArticleResponse(article));  //검색이 성공적이면 Article을 ArticleResponse로 정제해서 반환
    }

    // 아이디로 블로그 글 삭제해주는 매핑 클래스
    @DeleteMapping("/api/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        blogService.delete(id);

        return ResponseEntity.ok()
                .build();
    }

    // url에서 아이디를 받고 JSON으로 request까지 받아 블로그 글을 수정함
    @PutMapping("/api/articles/{id}")
    public ResponseEntity<Article> updateArticle(@PathVariable Long id, @RequestBody UpdateArticleRequest request) {
        Article updataArticle = blogService.update(id, request);

        return ResponseEntity.ok()
                .body(updataArticle);  //업데이트에 성공하면 업데이트한 객체를 반환함
    }
}
