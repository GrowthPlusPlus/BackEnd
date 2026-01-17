package me.jsu.springbootdeveloper.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static me.jsu.springbootdeveloper.domain.user.Role.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    해당 클래스는 UserApiController의 기능을 테스트하는 통합 테스트 클래스이다.
    최초 작성자: 여하늘
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserApiControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper; // 직렬화, 역직렬화를 위한 클래스

    @BeforeEach
    public void mockMvcSetUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
        userRepository.deleteAll(); // 모든 테스트를 시작하기 전에 기존 데이터 삭제
    }

    @DisplayName("findUserByNickname: 닉네임으로 유저를 검색한다.")
    @Test
    public void findUserByNickname() throws Exception {
        // given
        final String url = "/api/users/nickname/{nickname}";
        final String nickname = "testNickname";
        final String email = "test@email.com";

        User savedUser = userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .role(ROLE_USER)
                .build());

        // when
        final ResultActions result = mockMvc.perform(get(url, nickname)
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.nickname").value(nickname))
                .andExpect(jsonPath("$.role").value(ROLE_USER.name()));
    }

    @DisplayName("findUserByNickname: 존재하지 않는 닉네임으로 유저를 검색하면 404 에러가 발생한다.")
    @Test
    public void findUserByNicknameNotFound() throws Exception {
        // given
        final String url = "/api/users/nickname/{nickname}";
        final String nickname = "nonexistentNickname";

        // when
        final ResultActions result = mockMvc.perform(get(url, nickname)
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isNotFound()); // UserNotFoundException에 따라 404 반환 예상
    }
}


