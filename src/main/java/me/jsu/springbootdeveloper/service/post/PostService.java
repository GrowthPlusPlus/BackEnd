// 정성우

package me.jsu.springbootdeveloper.service.post;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.article.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.article.UpdateArticleRequest;
import me.jsu.springbootdeveloper.repository.post.PostRepository;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final ChallengeService challengeService;

    /*
        게시글을 추가하는 함수이다.
        request 객체와 userName 정보를 바탕으로 게시글을 생성하여 DB에 업데이트 한다.
     */
    public Article save(AddArticleRequest request, String userName) {
        // 유저 이메일로 user 객체를 검색한다.
        User user = userService.findByEmail(userName);

        // request로 온 챌린지 id로 post가 어떤 챌린지에 속하는지 검색한다.
        Challenge challenge = challengeService.findById(request.getChallengeId());

        // 챌린지 인증 시각을 정확하게 보기 위해 생성 시각과는 다른 인증 날짜 도메인을 생성한다.
        LocalDate postDate = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 서버에서 게시글 생성 시각을 명시적으로 설정한다.
        return postRepository.save(Article.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .challenge(challenge)
                .postDate(postDate)
                .build());
    }

    /*
        공개 상태의 게시글 일부를 조회하는 함수이다.
        리턴하는 값은 Article 객체를 가지고 있는 리스트이다.
     */
    public Page<Article> findAll(Pageable pageable) {
        return postRepository.findPublicChallengePostFeed(ChallengeVisibility.PUBLIC, pageable);
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

        if (!article.getUser().getEmail().equals(userName)) {  // 유저의 이메일과 게시글 작성자의 이메일을 비교한다.
            throw new ArticleAccessDeniedException("ARTICLE_ACCESS_DENIED");
        }
    }
}
