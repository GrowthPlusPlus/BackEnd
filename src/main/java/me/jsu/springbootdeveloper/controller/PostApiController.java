// 정성우

package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.dto.article.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.article.ArticleResponse;
import me.jsu.springbootdeveloper.dto.article.UpdateArticleRequest;
import me.jsu.springbootdeveloper.service.post.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.PageableDefault;

/*
    클라이언트의 요청 중, 챌린지 인증글인 post와 관련된 모든 요청을 처리해주는 컨트롤러이다.
 */
@RequiredArgsConstructor
@RestController  //http response body에 객체 데이터를 JSON형식으로 직렬하여 반환해주는 컨트롤러이다.
public class PostApiController {
    private final PostService postService;  //의존성을 주입하여 빈으로 등록된 PostService클래스의 함수를 사용한다.

    /*
       요청으로 온 http 메서드가 post이고, url이 아래와 같으면 매핑하여 함수를 실행한다.
       @RequestBody가 요청 본문 값인 JSON형태를 역직렬화하여 AddArticleRequest의 맴버변수에 매핑한다.
       @RestController가 있으므로 @RequestBody는 @PostMapping밑에 붙이지 않고 생략한다.

       해당 함수로 챌린지를 인증하기 위해 게시글을 만드는 역할을 한다.
     */
    @PostMapping("/api/articles")  // post형태는 REST API에서는 생성하는 경우에 사용한다.
    /*
        AddArticleRequest 맴버변수에 맞지 않는 request값이 들어오면 무시한다.
        principal은 스프링 시큐리티 환경에서 인증된 사용자임을 나타내는 매개변수이다.
     */
    public ResponseEntity<ArticleResponse> addArticle(
            @RequestBody AddArticleRequest request,
            // 안정성을 위하여 Principal 대신 사용한다.
            @AuthenticationPrincipal UserDetails userDetails) {
        Article savedArticle = postService.save(request, userDetails.getUsername());

        //DB에 저장한 정보를 그대로 다시 반환하는 것은 위험하므로 클라이언트에게 필요한 정보만으로 정제한다.
        ArticleResponse response = new ArticleResponse(savedArticle);

        // post가 성공하면, 게시글 정보를 응답 객체에 담아 JSON으로 반환하여 클라이언트가 바로 게시글 화면으로 넘어갈 수 있도록 한다.
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 리소스 생성 성공을 의미하는 상태 코드이다.
                .body(response);         // 정제한 dto값을 결과로 반환한다.
    }

    /*
        전체 게시글을 조회 한 뒤에 반환해주는 함수로, 챌린지 피드에서 모든 유저의 게시글을 최신순으로 보고 싶을 때 사용한다.
        페이징을 사용하여 기본적으론 한 번 검색에 10개의 게시글을 검색하도록 한다.
        또한 생성일을 기준으로 내림차순으로 검새갛여 최신 -> 과거 순으로 게시글을 검색한다.

        GET /api/articles?page=0&size=5 를 수정하여 클라이언트가 원하면 검색할 게시글 수를 조절할 수 있다.
     */
    @GetMapping("/api/articles")
    public ResponseEntity<Page<ArticleResponse>> findAllArticles(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ArticleResponse> articles = postService.findAll(pageable)
                .map(ArticleResponse::new);  // 검색한 Article값을 ArticleResponse로 변환한다.

        return ResponseEntity.ok(articles);  // 조회가 성공적이면 얻은 정보를 반환한다.
    }

    /*
        post id로 post 하나를 검색하여 반환하는 함수이다.
        url 경로에서 id값을 추출하여 사용한다.
     */
    @GetMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponse> findArticle(@PathVariable Long id) {  //검색할 id가 {id}로 들어온다.
        Article article = postService.findById(id);

        return ResponseEntity.ok(
                new ArticleResponse(article));  // 검색이 성공적이면 Article을 ArticleResponse로 정제해서 반환한다.
    }

    /*
        게시글 id로 글을 삭제하는 함수이다.
     */
    @DeleteMapping("/api/articles/{id}")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        postService.delete(id);

        return ResponseEntity.noContent().build();  // 삭제에 성공하면 204를 반환한다.
    }

    /*
        url에서 게시글 id를 받고, dto형태의 request를 받아 블로그 글을 수정한다.
        들어올 때는 json형태지만, 스프링이 자동으로 dto형태로 변환해준다.
     */
    @PatchMapping("/api/articles/{id}")
    public ResponseEntity<ArticleResponse> updateArticle(@PathVariable Long id, @RequestBody UpdateArticleRequest request) {
        Article updataArticle = postService.update(id, request);

        return ResponseEntity.ok(
                new ArticleResponse(updataArticle));  // 업데이트한 게시글을 정제하여 반환한다.
    }
}
