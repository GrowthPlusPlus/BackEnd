// 정성우

package me.jsu.springbootdeveloper.controller;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static me.jsu.springbootdeveloper.user.enumType.Role.ROLE_USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    MainHomeController의 기능을 테스트하는 통합 테스트 클래스이다.
    메인화면 조회 API의 정상 동작 여부를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MainHomeControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeParticipantRepository challengeParticipantRepository;

    @BeforeEach
    public void setUp() {
        //this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        //userRepository.deleteAll();  // 모든 테스트를 시작하기 전에 기존 데이터 삭제
    }

    @DisplayName("getMainHome: 메인화면 조회에 성공한다.")
    @Test
    public void getMainHome() throws Exception {
        // given
        final String url = "/api/mainHome";
        LocalDate targetDate = LocalDate.now();  // 오늘 날짜를 테스트 날짜로 사용

        User user = userRepository.save(User.builder()
                .email("test@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser")
                .build());

        Challenge challenge = challengeRepository.save(Challenge.builder()
                        .title("모각공")
                        .content("매일 공부")
                        .user(user)
                        .maxParticipantNumber(5)
                        .challengeVisibility(ChallengeVisibility.PUBLIC)
                        .startDate(LocalDate.now())
                        .endDate(LocalDate.now())
                        .requiredWeeklyCount(4)
                .build());

        ChallengeParticipant challengeParticipant = challengeParticipantRepository.save(ChallengeParticipant.builder()
                        .challenge(challenge)
                        .user(user)
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();  // http로 어떤 사용자가 로그인했는지 저장하는 장소를 가져옴
        // 현재 요청에 이 user를 로그인된 상태로 등록함
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("date", targetDate.toString())
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChallenges").exists())  // myChallenges 필드가 존재하는지 확인
                .andExpect(jsonPath("$.notificationNumber").exists())  // notificationNumber 필드가 존재하는지 확인
                .andDo(print());  // http응답 출력
    }

    @DisplayName("getMainHome: 과거 날짜로 메인화면 조회에 성공한다.")
    @Test
    public void getMainHomeWithPastDate() throws Exception {
        // given
        final String url = "/api/mainHome";
        LocalDate pastDate = LocalDate.now().minusDays(7);  // 7일 전 날짜

        User user = userRepository.save(User.builder()
                .email("test2@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser2")
                .build());

        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title("모각공")
                .content("매일 공부")
                .user(user)
                .maxParticipantNumber(5)
                .challengeVisibility(ChallengeVisibility.PUBLIC)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .requiredWeeklyCount(3)
                .build());

        ChallengeParticipant challengeParticipant = challengeParticipantRepository.save(ChallengeParticipant.builder()
                .challenge(challenge)
                .user(user)
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("date", pastDate.toString())
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChallenges").exists())
                .andExpect(jsonPath("$.notificationNumber").exists())
        .andDo(print());  // http응답 출력
    }

    @DisplayName("getMainHome: 미래 날짜로 메인화면 조회에 성공한다.")
    @Test
    public void getMainHomeWithFutureDate() throws Exception {
        // given
        final String url = "/api/mainHome";
        LocalDate futureDate = LocalDate.now().plusDays(7);  // 7일 후 날짜

        User user = userRepository.save(User.builder()
                .email("test3@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser3")
                .build());

        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title("모각공")
                .content("매일 공부")
                .user(user)
                .maxParticipantNumber(5)
                .challengeVisibility(ChallengeVisibility.PUBLIC)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .requiredWeeklyCount(3)
                .build());

        ChallengeParticipant challengeParticipant = challengeParticipantRepository.save(ChallengeParticipant.builder()
                .challenge(challenge)
                .user(user)
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("date", futureDate.toString())
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myChallenges").exists())
                .andExpect(jsonPath("$.notificationNumber").exists())
                .andDo(print());  // http응답 출력
    }

    @DisplayName("getMainHome: 날짜 파라미터가 없으면 400 에러가 발생한다.")
    @Test
    public void getMainHomeWithoutDate() throws Exception {
        // given
        final String url = "/api/mainHome";

        // 인증된 사용자를 설정하여 인증 단계를 통과시킨다. 파라미터 바인딩 단계에서 400 에러가 발생하도록 한다.
        User user = userRepository.save(User.builder()
                .email("test4@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser4")
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());  // 필수 파라미터가 없으므로 400 Bad Request
    }

    @DisplayName("getMainHome: 인증되지 않은 사용자는 401 에러가 발생한다.")
    @Test
    public void getMainHomeUnauthorized() throws Exception {
        // given
        final String url = "/api/mainHome";
        LocalDate targetDate = LocalDate.now();
        SecurityContextHolder.clearContext();  // SecurityContext를 비워서 인증되지 않은 상태로 만듦

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("date", targetDate.toString())
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());  // 인증되지 않은 사용자이므로 401 Unauthorized 또는 403 Forbidden
    }

    @DisplayName("getMainHome: 잘못된 날짜 형식이면 400 에러가 발생한다.")
    @Test
    public void getMainHomeWithInvalidDate() throws Exception {
        // given
        final String url = "/api/mainHome";
        String invalidDate = "2025-13-45";  // 잘못된 날짜 형식

        // 인증된 사용자를 설정하여 인증 단계를 통과시킨다. 파라미터 바인딩 단계에서 잘못된 날짜 형식으로 400 에러가 발생하도록 한다.
        User user = userRepository.save(User.builder()
                .email("test5@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser5")
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("date", invalidDate)
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isBadRequest());  // 잘못된 날짜 형식이므로 400 Bad Request
    }
}

