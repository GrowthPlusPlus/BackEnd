// 정성우

package me.jsu.springbootdeveloper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.dto.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.UpdateArticleRequest;
import me.jsu.springbootdeveloper.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class PostService {
    private final PostRepository postRepository;

    /*
        게시글을 추가하는 함수이다.
        request 객체와 userName 정보를 바탕으로 게시글을 생성하여 DB에 업데이트 한다.
     */
    public Article save(AddArticleRequest request, String userName) {
        return postRepository.save(request.toEntity(userName));
    }

    /*
        게시글 전체를 조회하는 함수이다.
        리턴하는 값은 Article 객체를 가지고 있는 리스트이다.
     */
    public Page<Article> findAll(Pageable pageable) {
        return postRepository.findAll(pageable);
    }

    /*
        게시글을 게시글 id로 조회하는 함수이다.
     */
    public Article findById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));
    }

    /*
        게시글 하나를 삭제하는 함수로, 게시글 작성자 id가 요청자 id와 다르면 삭제하지 않는다.

        다음은 @Transactional를 사용했을 때의 효과이다.
        1) findById() 호출 시 엔티티는 영속성 컨텍스트에 등록되어
           JPA의 관리 대상이 된다.

        2) 엔티티의 필드를 변경하면 JPA는 즉시 DB에 반영하지 않고,
           영속성 컨텍스트 내에서 변경된 상태를 유지한다.

        3) 트랜잭션 커밋 시점에 flush가 발생하고,
           JPA는 스냅샷과 현재 상태를 비교(dirty checking)하여
           변경된 엔티티에 대해 UPDATE SQL을 생성하고 실행한다.

        따라서 @Transactional는 DB 작업의 원자성, 일관성, 격리성, 지속성을 보장한다.
     */
    @Transactional
    public void delete(Long id) {
        Article article = postRepository.findById(id)  // 삭제할 글을 DB에서 찾는다.
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));
        authorizeArticleAuthor(article);  // 글 작성자인지 확인한다.
        postRepository.deleteById(id);  // 맞다면 DB에서 삭제한다.
    }

    /*
        게시글을 수정하는 함수로, 게시글 작성자 id가 요청자 id와 다르면 삭제하지 않는다.
     */
    @Transactional
    public Article update(long id, UpdateArticleRequest request) {
        Article article = postRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("ARTICLE_NOT_FOUND_FOR_ID"));

        authorizeArticleAuthor(article);  // 글 작성자인지 확인한다.
        // 위에서 찾은 게시글을 UpdateArticleRequest 객체의 값으로 업데이트 한다.
        article.update(request.getTitle(), request.getContent());

        // @Transactional가 엔티티 수정을 감지하고 자동으로 DB에 값을 업데이트 하므로, 수정된 게시글 정보만 반환한다.
        return article;
    }

    /*
        계시글을 작성한 유저인지 확인해주는 함수이다.
        email을 기준으로 비교한다.
     */
    private static void authorizeArticleAuthor(Article article) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();  // 현재 로그인한 유저의 이메일을 가져온다.

        if (!article.getAuthor().equals(userName)) {  // 유저의 이메일과 게시글 작성자의 이메일을 비교한다.
            throw new ArticleAccessDeniedException("ARTICLE_ACCESS_DENIED");
        }
    }
}
