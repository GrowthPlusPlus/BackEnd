// 정성우

package me.jsu.springbootdeveloper.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

/*
    Spring Security의 보안 규칙 인가와 필터 체인 구성을 정의한다.

    1. 세션/폼로그인 같은 웹 방식 비활성화(앱 + JWT 구조에 맞춤)
    2. JWT 인증 필터를 Security Filter Chain에 끼워 넣기
    3. 어떤 URL은 인증 없이 허용(permitAll), 나머지 API는 인증 필요(authenticated)
 */
@RequiredArgsConstructor
@Configuration  // 해당 클래스가 스프링 설정 클래스임을 의미한다. 즉 @Bean 등록 가능한다.
public class OAuthSecurityConfig {
    private final TokenProvider tokenProvider;  // JWT 검증, Authentication 생성을 위해 의존성을 추가한다.

    /*
        스프링 시큐리티 기능을 비활성화할 요청들을 정의하는 함수이다.
        web.ignoring() = 보안 체인을 아예 타지 않는다.
        permitAll() = 보안 체인은 타지만 허용한다.
     */
    @Bean
    public WebSecurityCustomizer configure() {
        return (web) -> web.ignoring()  // 아래의 요청들은 스프링 시큐티리가 건드리지 않는다. 즉 세큐리티 필터를 무시한다.
                .requestMatchers(toH2Console())  // h2데베 콘솔은 보안 없이 접근 가능하다. (h2는 테스트를 위해 사용. 후에 비활성화)
                .requestMatchers(
                        new AntPathRequestMatcher("/img/**"),  //해당 정적 파일들은 인증 절차에서 제외한다.
                        new AntPathRequestMatcher("/css/**"),
                        new AntPathRequestMatcher("/js/**")
                );
    }

    /*
        서버의 보안 정책과 필터 순서를 위한 함수이다.
        OAuth 로그인/재발급은 열어두고, 나머지 API는 JWT 있어야만 접근하도록 한다.
        PKCE 방식으로 소통하므로 스프링 시큐리티에서 따로 설정하지 않는다.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 토큰 방식으로 인증을 하기 때문에 기존에 사용하던 폼 로그인, 세션 비활성화한다.
        return http
                // 서버가 세션을 만드는 경우에만 CSRF보호가 필수이므로 비활성화 한다.
                .csrf(AbstractHttpConfigurer::disable)
                // 사용자 아이디 + 비밀번호를 매 요청마다 보내는 기능으로 우리는 매 요청마다 토큰을 보낼 것이므로 비활성화 한다.
                .httpBasic(AbstractHttpConfigurer::disable)
                //웹 로그인 방식 대신 JWT + OAuth2 사용하므로 비활성화 한다.
                .formLogin(AbstractHttpConfigurer::disable)
                // 토큰 기반에선 토큰 무효화/삭제 정책으로 로그아웃 하므로 세션 기반 로그아웃 방식은 비활성화 한다.
                .logout(AbstractHttpConfigurer::disable)
                // 인증은 JWT토큰으로만 수행하므로 서버의 세션을 비활성화 한다.
                .sessionManagement(management ->
                        management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 헤더를 확인할 커스텀(jwt) 필터를 추가 한다. 헤더가 Bearer JWT 형태인지 검사하는 체인으로 스프링이 만든 필터 체인에 내 필터를 삽입한다.
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                // URL에 따라 보안 인증 여부에 따른 접근 권한을 설정한다.
                .authorizeHttpRequests(auth -> auth
                        // 테스트용 토큰 발급 API는 인증 없이 접근 가능하도록 허용
                        .requestMatchers(new AntPathRequestMatcher("/api/test/token")).permitAll()
                        // 리프레시 토큰 기반으로 엑세스 토큰을 재발급 해주기도 한다.
                        .requestMatchers(new AntPathRequestMatcher("/api/token")).permitAll()
                        // 사용자가 Google OAuth(PKCE)로 로그인할 때 이다.
                        // OAuth 로그인/토큰 교환 엔드포인트이다. 즉 jwt인 엑세스 토큰, 리프레시 토큰이 아직 없는 상황이다.
                        // 엑세스 토큰, 리프레시 토큰 생성 등의 일을 한다.
                        .requestMatchers(new AntPathRequestMatcher("/api/oauth/**")).permitAll()
                        // 위의 두 케이스를 제외한 api는 모두 인증이 필요하다.
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated()
                        // api가 아닌 화면/정적 페이지 등은 허용한다.
                        .anyRequest().permitAll())
                // 토큰이 없으면 컨트롤러에 진입하지 못하도록 401응답을 만들어준다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .build();
    }

    /*
        위의 addFilterBefore에서 TokenAuthenticationFilter 클래스를 스프링 빈으로 등록한다.
        즉 모든 요청에 대해 JWT 토큰의 유효성을 검사한다.
        생성자에 tokenProvider를 받아 내가 생성한 커스텀 필터에서 토큰 생성, 권한 검사 등을 가능하게 한다.
     */
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    /*
        회원가입 시 비밀번호를 hash로 변환해준다.
        현재는 OAuth만 사용하므로 사용하지 않는다.
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
