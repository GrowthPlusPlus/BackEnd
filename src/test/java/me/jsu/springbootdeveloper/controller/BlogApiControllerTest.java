package me.jsu.springbootdeveloper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.jsu.springbootdeveloper.domain.Article;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.AddArticleRequest;
import me.jsu.springbootdeveloper.dto.UpdateArticleRequest;
import me.jsu.springbootdeveloper.repository.PostRepository;
import me.jsu.springbootdeveloper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest  //기본 변수들의 autowired가 가능해지게 하는 역할
@AutoConfigureMockMvc  //MockMvc 빈을 만들어주는 역할
class BlogApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;  //직렬화 (자바 -> JSON)과 역직렬화 (JSON -> 자바)를 위해 사용

    @Autowired
    WebApplicationContext context;

    @Autowired
    PostRepository postRepository;

    @Autowired
    UserRepository userRepository;

    User user;

    @BeforeEach
    public void mockMvcSetup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
        postRepository.deleteAll();
    }

    @BeforeEach
    void setSecurityContext() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        SecurityContext context = SecurityContextHolder.getContext();  // http로 어떤 사용자가 로그인했는지 저장하는 장소를 가져옴
        // 현재 요청에 이 user를 로그인된 상태로 등록함
        context.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));
    }

    @DisplayName("addArticle: 블로그 글 추가에 성공한다.")
    @Test
    public void addArticle() throws Exception {
        //given 데이터 준비
        final String url = "/api/articles";
        final String title = "title";
        final String content = "content";
        final AddArticleRequest userRequest = new AddArticleRequest(title, content);  //JSON방식이 아닌 직접 생성

        //자바 객체로 만들었으므로 JSON형식으로 직렬화
        final String requestBody = objectMapper.writeValueAsString(userRequest);

        Principal principal = Mockito.mock(Principal.class);  // 가짜 객체 생성
        Mockito.when(principal.getName()).thenReturn("username");  // getName을 호출하면 항상 username반환

        //when post요청
        ResultActions result = mockMvc.perform(post(url)        //ResultActions객체로 응답을 검증 가능함
                .contentType(MediaType.APPLICATION_JSON_VALUE)  //요청 본문이 JSON형태라고 알려줌 (앞에서 직렬화를 했으므로)
                        .principal(principal)
                .content(requestBody));  //앞서서 직렬화 요청을 @RequestBody AddArticleRequest로 매핑함

        //then 검증
        result.andExpect(status().isCreated());  //실제로 생성 되었는지 검사

        List<Article> articles = postRepository.findAll();  //데베 내용 모두 가져오기

        assertThat(articles.size()).isEqualTo(1);  //하나만 넣을 예정이므로 하나만 있는지 검사
        assertThat(articles.get(0).getTitle()).isEqualTo(title);  //첫번째 객체의 타이틀 값이 title인지 검사
        assertThat(articles.get(0).getContent()).isEqualTo(content);  //첫번째 객체의 타이틀 값이 content인지 검사
    }

    @DisplayName("findAllArticles: 블로그 글 목록 조회에 성공한다.")
    @Test
    public void findAllArticles() throws Exception {
        //given 데이터 준비
        final String url = "/api/articles";
//        final String title = "title";
//        final String content = "content";
        Article saveArticle = createDefaultArticle();

        //검색하기 위해 미리 데이터를 데이터베이스에 넣음
//        blogRepository.save(Article.builder()
//                .title(title)
//                .content(content)
//                .build());

        //when
        final ResultActions resultActions = mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON_VALUE));   //고객이 서버에게 원하는 응답 타입이 JSON임

        //then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value(saveArticle.getContent()))  //응답의 첫번째 요소의 content필드 값이 this.content와 같은지 검사
                .andExpect(jsonPath("$[0].title").value(saveArticle.getTitle()));

    }

    @DisplayName("findArticle: 블로그 글 조회에 성공한다.")
    @Test
    public void findArticle() throws Exception {
        //given
        final String url = "/api/articles/{id}";
//        final String title = "title";
//        final String content = "content";
        Article savedArticle = createDefaultArticle();

//        Article savedArticle = blogRepository.save(Article.builder()  //미리 데이터를 데베에 저장하고 객체를 저장함
//                .title(title)
//                .content(content)
//                .build());

        //when  정보 검색
        final ResultActions resultActions = mockMvc.perform(get(url, savedArticle.getId()));

        //then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(savedArticle.getContent()))
                .andExpect(jsonPath("$.title").value(savedArticle.getTitle()));
    }

    @DisplayName("deleteArticle: 블로그 글 삭제에 성공한다.")
    @Test
    public void deleteArticle() throws Exception {
        //given
        final String url = "/api/articles/{id}";
//        final String title = "title";
//        final String content = "content";
        Article savedArticle = createDefaultArticle();

//        Article savedArticle = blogRepository.save(Article.builder()  //미리 데이터를 데베에 저장하고 객체를 저장함
//                .title(title)
//                .content(content)
//                .build());

        //when
        mockMvc.perform(delete(url, savedArticle.getId()))  //아까 저장한 객체의 아이디를 이용해 삭제
                .andExpect(status().isOk());

        //then
        List<Article> articles = postRepository.findAll();  //전체 글을 검색

        assertThat(articles).isEmpty();
    }

    @DisplayName("updateArticle: 블로그 글 수정에 성공한다.")
    @Test
    public void updateArticle() throws Exception {
        //given
        final String url = "/api/articles/{id}";
//        final String title = "title";
//        final String content = "content";
        Article savedArticle = createDefaultArticle();

//        Article savedArticle = blogRepository.save(Article.builder()  //미리 데이터를 데베에 저장하고 객체를 저장함
//                .title(title)
//                .content(content)
//                .build());

        final String newTitle = "new title";
        final String newContent = "new content";

        UpdateArticleRequest request = new UpdateArticleRequest(newTitle, newContent);  //요청을 만듦

        //when
        ResultActions result = mockMvc.perform(put(url, savedArticle.getId())  //getId로 얻은 객체를 request값으로 업데이트
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(request)));  //request가 객체이므로 JSON형태로 직렬화

        //then
        result.andExpect(status().isOk());

        Optional<Article> article = postRepository.findById(savedArticle.getId());  //아까 업데이트 한 객체를 검색하여 article에 넣음
        if (article.isPresent()) {
            assertThat(article.get().getTitle()).isEqualTo(newTitle);
            assertThat(article.get().getContent()).isEqualTo(newContent);
        }
    }

    private Article createDefaultArticle() {
        return postRepository.save(Article.builder()
                .title("title")
                .author(user.getUsername())
                .content("content")
                .build());
    }
}
