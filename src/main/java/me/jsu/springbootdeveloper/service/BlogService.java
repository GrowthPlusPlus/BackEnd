package me.jsu.springbootdeveloper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.dto.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.UpdateArticleRequest;
import me.jsu.springbootdeveloper.repository.BlogRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor  //final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 함
@Service  //빈으로 등록
public class BlogService {
    //생성자가 하나이므로 @Autowired는 생략
    private final BlogRepository blogRepository;  //RequiredArgsConstructor로 초기화 되는 맴버변수 -> 데베 접근을 위해 사용

    // 블로그 글 추가 메서드
    public Article save(AddArticleRequest request, String userName) {
        return blogRepository.save(request.toEntity(userName));  //request 객체에 있는 맴버변수를 데이터베이스에 넣음
    }

    // 블로그 글을 전체 조회
    public List<Article> findAll() {  //리턴하는 값이 Article 객체를 가지고 있는 리스트임
        return blogRepository.findAll();
    }

    //블로그 글 하나를 조회
    public Article findById(Long id) {
        return blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found: " + id));  //쿼리로 검색하지 못한 경우
    }

    //블로그 글 하나를 삭제 (작성자 아이디와 다르면 삭제하지 않음)
    public void delete(Long id) {
        Article article = blogRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found: " + id));  // 삭제할 글을 DB에서 찾음
        authorizeArticleAuthor(article);  // 글 작성자인지 확인
        blogRepository.deleteById(id);  // 맞으면 삭제
    }

    //블로그 글을 수정

    /*
    1) findById() → 엔티티 영속 상태
    JPA가 해당 객체의 변경을 자동 추적(dirty checking)

    2) 엔티티 변경 시 JPA는 내부적으로 “변경 감지”
    아직 DB에는 반영 안 됨
    단지 “변화가 있었다”는 메모만 기록

    3) 트랜잭션(@Transactional) 커밋 시점에 UPDATE SQL 실행
    이 과정이 flush
    이때 디비에 수정 내용 반영됨
     */

    @Transactional  //데이터베이스의 값을 바꾸기 위해 묶은 작업의 단위로 멱등 연산을 보장함 (A계좌 돈을 출금 -> 고장 -> B계좌 송금 실패 방지)
    public Article update(long id, UpdateArticleRequest request) {
        Article article = blogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("not found: + id"));

        authorizeArticleAuthor(article);
        //request에서 얻은 UpdateArticleRequest 객체의 값으로 검색해서 얻은 Article을 업데이트 함
        article.update(request.getTitle(), request.getContent());

        return article;
    }

    // 계시글을 작성한 유저인지 확인
    private static void authorizeArticleAuthor(Article article) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();  // 현재 로그인한 유저의 아이디를 가져옴

        if (!article.getAuthor().equals(userName)) {  // 아이디와 article author의 이름 비교
            throw new IllegalArgumentException("not authorized");
        }
    }
}
