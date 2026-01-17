// 정성우

package me.jsu.springbootdeveloper.controller;

import me.jsu.springbootdeveloper.domain.notification.Notification;
import me.jsu.springbootdeveloper.domain.notification.NotificationType;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.repository.NotificationRepository;
import me.jsu.springbootdeveloper.repository.user.UserRepository;
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

import static me.jsu.springbootdeveloper.domain.user.Role.ROLE_USER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    NotificationController의 기능을 테스트하는 통합 테스트 클래스이다.
    알림 조회 API의 정상 동작 여부를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    public void setUp() {
        //this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        //notificationRepository.deleteAll();  // 모든 테스트를 시작하기 전에 기존 알림 데이터 삭제
        //userRepository.deleteAll();  // 모든 테스트를 시작하기 전에 기존 사용자 데이터 삭제
    }

    @DisplayName("getNotification: 알림 조회에 성공한다.")
    @Test
    public void getNotification() throws Exception {
        // given
        final String url = "/api/notification";
        int page = 0;  // 첫 번째 페이지

        User user = userRepository.save(User.builder()
                .email("test@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser")
                .build());

        // 테스트용 알림 데이터 생성
        Notification notification1 = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM)
                .title("시스템 알림")
                .message("테스트 알림 메시지입니다.")
                .build());

        Notification notification2 = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.LIKE)
                .title("좋아요 알림")
                .message("누군가 당신의 게시글을 좋아합니다.")
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();  // http로 어떤 사용자가 로그인했는지 저장하는 장소를 가져옴
        // 현재 요청에 이 user를 로그인된 상태로 등록함
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("page", String.valueOf(page))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myNotification").exists())  // myNotification 필드가 존재하는지 확인
                .andExpect(jsonPath("$.hasNext").exists())  // hasNext 필드가 존재하는지 확인
                .andExpect(jsonPath("$.myNotification").isArray())  // myNotification이 배열인지 확인
                .andDo(print());  // http응답 출력
    }

    @DisplayName("getNotification: 알림이 없는 경우 빈 리스트를 반환한다.")
    @Test
    public void getNotificationEmpty() throws Exception {
        // given
        final String url = "/api/notification";
        int page = 0;

        User user = userRepository.save(User.builder()
                .email("test2@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser2")
                .build());

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("page", String.valueOf(page))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myNotification").exists())
                .andExpect(jsonPath("$.myNotification").isArray())
                .andExpect(jsonPath("$.myNotification").isEmpty())  // 빈 배열인지 확인
                .andExpect(jsonPath("$.hasNext").value(false))  // 다음 페이지가 없음을 확인
                .andDo(print());
    }

    @DisplayName("getNotification: 두 번째 페이지 조회에 성공한다.")
    @Test
    public void getNotificationSecondPage() throws Exception {
        // given
        final String url = "/api/notification";
        int page = 1;  // 두 번째 페이지

        User user = userRepository.save(User.builder()
                .email("test3@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser3")
                .build());

        // 25개의 알림을 생성하여 두 번째 페이지에 데이터가 있도록 함 (한 페이지당 20개)
        for (int i = 0; i < 25; i++) {
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(NotificationType.SYSTEM)
                    .title("알림 " + i)
                    .message("테스트 알림 메시지 " + i)
                    .build());
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("page", String.valueOf(page))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myNotification").exists())
                .andExpect(jsonPath("$.myNotification").isArray())
                .andExpect(jsonPath("$.hasNext").exists())
        .andDo(print());
    }

    @DisplayName("getNotification: page 파라미터가 없으면 400 에러가 발생한다.")
    @Test
    public void getNotificationWithoutPage() throws Exception {
        // given
        final String url = "/api/notification";

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
        result.andExpect(status().isBadRequest())  // 필수 파라미터가 없으므로 400 Bad Request
        .andDo(print());
    }

    @DisplayName("getNotification: 인증되지 않은 사용자는 401 에러가 발생한다.")
    @Test
    public void getNotificationUnauthorized() throws Exception {
        // given
        final String url = "/api/notification";
        int page = 0;
        SecurityContextHolder.clearContext();  // SecurityContext를 비워서 인증되지 않은 상태로 만듦

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("page", String.valueOf(page))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result.andExpect(status().isUnauthorized());  // 인증되지 않은 사용자이므로 401 Unauthorized
    }

    @DisplayName("getNotification: 읽지 않은 알림과 읽은 알림이 모두 조회된다.")
    @Test
    public void getNotificationWithReadStatus() throws Exception {
        // given
        final String url = "/api/notification";
        int page = 0;

        User user = userRepository.save(User.builder()
                .email("test5@email.com")
                .password("test")
                .role(ROLE_USER)
                .nickname("testUser5")
                .build());

        // 읽지 않은 알림
        Notification unreadNotification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.COMMENT)
                .title("댓글 알림")
                .message("새로운 댓글이 달렸습니다.")
                .build());

        // 읽은 알림
        Notification readNotification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(NotificationType.LIKE)
                .title("좋아요 알림")
                .message("누군가 좋아요를 눌렀습니다.")
                .build());
        readNotification.markAsRead();  // 읽음 처리
        notificationRepository.save(readNotification);

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities()));

        // when
        final ResultActions result = mockMvc.perform(get(url)
                .param("page", String.valueOf(page))
                .accept(MediaType.APPLICATION_JSON));

        // then
        result
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myNotification").exists())
                .andExpect(jsonPath("$.myNotification").isArray())
                .andExpect(jsonPath("$.myNotification.length()").value(2))  // 두 개의 알림이 조회되는지 확인
        .andDo(print());
    }
}

