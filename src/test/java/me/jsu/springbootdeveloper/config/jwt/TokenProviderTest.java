package me.jsu.springbootdeveloper.config.jwt;

import io.jsonwebtoken.Jwts;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

@SpringBootTest
public class TokenProviderTest {
    @Autowired
    private TokenProvider tokenProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtProperties jwtProperties;

    // generationToken() 검증
    @DisplayName("generationToken(): 유저 정보와 만료기간을 전달하여 토큰 생성")
    @Test
    void generationToken() {
        //given -> 유저 정보를 임의로 생성
        User testUser = userRepository.save(User.builder()
                .email("user@gmail.com")
                .password("test")
                .build());

        //when -> 유저정보와 만료일로 토큰 생성
        String token = tokenProvider.generateToken(testUser, Duration.ofDays(14));

        //then -> 생성됐는지 token을 파싱하여 검사
        Long userId = Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);

        // 내가 처음 만든 유저 정보랑 파싱해서 얻은 유저 정보랑 같은지 확인
        assertThat(userId).isEqualTo(testUser.getId());
    }

    // validToken 검증 테스트
    @DisplayName("validToken(): 만료된 토큰일 때, 유효성 검증에 실패함")
    @Test
    void validToken_in() {
        // given -> 7일이 지난 시간으로 만료된 토큰을 createToken에서 생성
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis())).build().createToken(jwtProperties);

        //when
        boolean result = tokenProvider.validToken(token);  // 실제로 검사

        //then -> 결과 확인
        assertThat(result).isFalse();
    }

    @DisplayName("validToken(): 유요한 토큰일 때에 유효성 검증에 성공함")
    @Test
    void valid() {
        //given
        String token = JwtFactory.withDefaultValues().createToken(jwtProperties);

        //when
        boolean result = tokenProvider.validToken(token);

        //then
        assertThat(result).isTrue();
    }

    //getAuthentication 검증 테스트
    @DisplayName("getAuthentication: 토큰 기반으로 인증 정보를 가져옴")
    @Test
    void getAuthentication() {
        //given
        String userEmail = "user@email.com";
        String token = JwtFactory.builder()
                .subject(userEmail)
                .build()
                .createToken(jwtProperties);

        //when
        Authentication authentication = tokenProvider.getAuthentication(token);

        //then
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);

        /*
        authentication.getPrincipal() 내에 JWT에서 꺼낸 사용자 이메일(=username), 권한(ROLE_USER) 등을 가진 UserDetails 객체가 들어 있다.
         */
    }

    // getUserId() 검증 테스트
    @DisplayName("getUserId: 토큰으로 유저 id를 가져옴")
    @Test
    void getUserId() {
        //given
        Long userId = 1L;
        String token = JwtFactory.builder()
                .claims(Map.of("id", userId))
                .build()
                .createToken(jwtProperties);

        //when
        Long userIdByToken = tokenProvider.getUserId(token);

        //then
        assertThat(userIdByToken).isEqualTo(userId);
    }
}
