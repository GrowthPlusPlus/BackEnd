//package me.jsu.springbootdeveloper.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.ProviderManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//
//import javax.swing.*;
//
//import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;
//
//@Configuration
///*
//@Configuration 어노테이션은 해당 클래스를 스프링의 설정 클래스로 인식하게 한다.
//이 클래스를 통해 빈(Bean) 설정 등을 할 수 있으며, 스프링 컨테이너가 이 클래스를 읽고 관리한다.
//*/
//
//@EnableWebSecurity
///*
//@EnableWebSecurity 어노테이션은 스프링 시큐리티를 활성화하는 역할을 한다.
//이 어노테이션이 선언된 클래스를 기반으로 사용자가 WebSecurityConfigurer를 상속받아
//시큐리티 관련 설정을 직접 커스터마이징 할 수 있다. 즉, 인증/인가 등 웹 보안 설정을 개발자가 제어할 수 있게 해준다.
//*/
//@RequiredArgsConstructor
//public class WebSecurityConfig {
//
//    private final UserDetailsService userDetailsService;
//
//    // 스프링 시큐리티 기능 비활성화
//    @Bean  // 이 함수에서 리턴하는 값을 빈으로 등록함 -> 복잡한 초기화 과정에 필요할 때 사용함
//    public WebSecurityCustomizer configure() {
//        return (web) -> web.ignoring()  //ignore 뒤 요청은 security가 관여하지 않음
//                .requestMatchers(toH2Console())  //h2-console 경로를 세큐리티가 무시함 -> h2 콘솔을 로그인 인증 없이 접근 가능
//                .requestMatchers(new AntPathRequestMatcher("/static/**"));  //static패키지에 있는 리소스를 filterChain밖으로 제외시켜 즉시 제공 가능함
//    }
//
//    // 특정 http 요청에 대한 웹 기반 보안 구성
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        return http
//                .authorizeRequests(auth -> auth  // 요청마다 허용할지 인증을 요구할지 판단함
//                        .requestMatchers(new AntPathRequestMatcher("/login"),
//                                         new AntPathRequestMatcher("/signup"),
//                                         new AntPathRequestMatcher("/user")).permitAll()  // 위 3개는 인증 없이 접근 가능
//                        .anyRequest().authenticated())  // 나머지 리퀘스트는 인증 요구
//                        .formLogin(formLogin -> formLogin.loginPage("/login")  // 폼 기반 로그인 설정으로 /login가 인증되지 않았을때 초기 화면이 됨
//                        .defaultSuccessUrl("/articles"))  // 로그인 성공시 /articles 페이지로 이동
//                        .logout(logout -> logout   // 로그아웃 설정
//                        .logoutSuccessUrl("/login").invalidateHttpSession(true))  // 로그아웃 성공 시 /login페이지로 이동, invalid로 로그인 정보 모두 제거
//                .csrf(AbstractHttpConfigurer::disable).build();  //csrf보호 비활성화 (개발중이기 때문), build()로 SecurityFilterChain객체를 생성하여 security 필터에 등록
//    }
//
//    // 인증 관리자 관련 설정
//    @Bean  // @Bean 애너테이션: 해당 메서드가 반환하는 객체를 스프링 컨테이너에 빈으로 등록한다는 의미야
//    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder bCryptPasswordEncoder,
//                                                       UserDetailsService userDetailsService) throws Exception {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();  // 사용자 인증을 수행해주는 객체임 (DB에서 사용자 정보를 읽어 비밀번호 매칭 -> 인증 성공/실패 판단)
//        authProvider.setUserDetailsService(userDetailsService);  // 사용자 정보를 가져와서 아래에서 비교함
//        authProvider.setPasswordEncoder(bCryptPasswordEncoder);  // 저장된 비밀번호와 입력된 비밀번호를 비교함 (BCrypt가 비빌번호 비교 인증)
//        return new ProviderManager(authProvider);  // 인증을 처리할 매니저를 “생성해서 반환”하는 코드 -> 매니저가 내부적으로 위 코드를 실행해 인증 처리
//    }
//
//    // 패스워드 인코더로 사용할 빈 등록
//    //BCrypt 해시 함수를 사용하여 비밀번호를 암호화(encode)하고, 비교(matches)하는 역할을 한다.
//    //authProvider.setPasswordEncoder(bCryptPasswordEncoder);에서 의존성 주입으로 가져와야 하므로 빈으로 등록함
//    //SCrypt나 Argon2 등 다른 암호화 전략을 사용할 수도 있으므로 사용자가 미리 선택하는 것 -> 여기선 BCrypt 사용
//    @Bean
//    public BCryptPasswordEncoder bCryptPasswordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}

