
package me.jsu.springbootdeveloper.token.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.user.enumType.Role;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class TestTokenApiController {

    private final TokenProvider tokenProvider;
    private final UserRepository userRepository;

    /*
        로컬 테스트를 위해 특정 이메일을 가진 유저의 엑세스 토큰을 강제로 생성한다.
        실제 OAuth 로그인을 거치지 않고 Postman 테스트를 하기 위함이다.
        사용법: GET /api/test/token?email=test@gmail.com
     */
    @GetMapping("/api/test/token")
    public String createTestToken(@RequestParam String email) {
        // 도메인 규칙에 맞는 유저 생성 로직 적용
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String defaultNickname = email.split("@")[0];

                    return userRepository.save(User.builder()
                            .email(email)
                            // SQL 예시에 있던 인코딩된 기본 패스워드
                            .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy8pL5O")
                            .nickname(defaultNickname)
                            .role(Role.ROLE_USER) // 도메인 Role 적용
                            .build()); // 생성자 내부에서 userStatus = UserStatus.NEW 가 설정됨
                });

        // 1년 동안 유효한 토큰 생성 (원할한 테스트를 위함)
        return tokenProvider.generateToken(user, Duration.ofDays(365));
    }
}