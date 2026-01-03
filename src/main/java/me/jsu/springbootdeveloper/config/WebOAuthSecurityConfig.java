package me.jsu.springbootdeveloper.config;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.config.oauth.OAuth2AuthorizationRequestBasedOnCookieRepository;
import me.jsu.springbootdeveloper.config.oauth.OAuth2SuccessHandler;
import me.jsu.springbootdeveloper.config.oauth.OAuth2UserCustomService;
import me.jsu.springbootdeveloper.repository.RefreshTokenRepository;
import me.jsu.springbootdeveloper.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@RequiredArgsConstructor
@Configuration  // 이 클래스는 설정 클래스라고 선언함 -> @Bean 등록 가능
public class WebOAuthSecurityConfig {
    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Bean
    public WebSecurityCustomizer configure() {  // 스프링 시큐리티 기능 비활성화
        return (web) -> web.ignoring()  // 아래의 요청들은 스프링 시큐티리가 건드리지 않음 -> 세큐리티 필터 무시
                .requestMatchers(toH2Console())  // h2데베 콘솔은 보안 없이 접근 가능
                .requestMatchers(
                        new AntPathRequestMatcher("/img/**"),  //정적 파일은 인증 절차가 없음
                        new AntPathRequestMatcher("/css/**"),
                        new AntPathRequestMatcher("/js/**")
                );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 토큰 방식으로 인증을 하기 때문에 기존에 사용하던 폼 로그인, 세션 비활성화
        return http
                .csrf(AbstractHttpConfigurer::disable)  // 서버가 세션을 만드는 경우에만 CSRF보호가 필수임
                .httpBasic(AbstractHttpConfigurer::disable)  // 사용자 아이디 + 비밀번호를 매 요청마다 보내는 기능으로 우리는 매 요청마다 토큰을 보낼 것이므로 비활성화
                .formLogin(AbstractHttpConfigurer::disable)  //로그인 폼 대신 JWT + OAuth2 사용
                .logout(AbstractHttpConfigurer::disable)  // 세션 로그아웃이 필요 없으므로 비활성화
                .sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 서버가 세션을 만들지 않음 -> 인증은 JWT토큰으로만 수행
                //헤더를 확인할 커스텀 필터 추가 -> 헤더가 Bearer JWT 형태인지 검사함
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)  // user.. 는 formLogin을 쓰지 않으므로 JWT 인증을 가로채기 위함
                // 토큰 재발급 URL은 보안 인증 없이 접근 가능하도록 설정. 나머지 API URL은 인증 필요/ 나머지 보안과 상관없는 api가 없는 주소는 그냥 허용
                .authorizeRequests(auth -> auth.requestMatchers(new AntPathRequestMatcher("/api/token")).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/**")).authenticated().anyRequest().permitAll())
                //OAuth2 로그인 시작 URL 지정, 실제 Google 로그인 페이지가 아닌 우리 서비스의 진입 페이지임
                .oauth2Login(oauth2 -> oauth2.loginPage("/login")
                        // Authorization 요청과 관련된 상태 저장
                        .authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                                .authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository()))
                        .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint.userService(oAuth2UserCustomService))
                        // 인증 성공 시 실행할 핸들러
                        .successHandler(oAuth2SuccessHandler()))
                // /api로 시작하는 url인 경우 401상태 코드를 반환하도록 예외 처리 -> 서버에만 401만 뜨고 유저는 무반응
                .exceptionHandling(exceptionHandling -> exceptionHandling.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")
                ))
                .build();
    }

    //OAuth 로그인 성공 후 실행되는 핸들러
    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProvider, refreshTokenRepository, oAuth2AuthorizationRequestBasedOnCookieRepository(), userService);
    }
    
    // 모든 요청에 대해 JWT 토큰을 검사함 -> 유효성 검사
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    // OAuth2 로그인 과정에서 필요한 정보를 세션 대신 쿠키에 저장해주는 메서드 (SessionCreationPolicy.STATELESS 때문에 세션이 없음)
    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }

    // 회원가입 시 비밀번호를 hash로 변환해줌
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
