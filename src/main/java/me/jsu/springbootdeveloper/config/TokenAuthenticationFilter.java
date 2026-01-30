// 정성우

package me.jsu.springbootdeveloper.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/*
    OAuth가 끝난 뒤 JWT로 보호된 API를 호출할 때마다 거치는 필터이다.
    즉 JWT가 붙은 요청을 인증된 요청으로 바꿔주는 필터이다.
    permitAll, authenticated와 관계없이 jwt는 이 필터를 거치게 되고 결과를 쓰냐 마냐가 permitAll과 authenticated에 따라 다르다.
    해당 클래스는 증거를 올려두는 역할일 뿐이고 실제 결과 판단은 스프링 시큐리티가 수행한다.
 */
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final static String HEADER_AUTHORIZATION = "Authorization";  // 요청 해더의 키 이름
    private final static String TOKEN_PREFIX = "Bearer";  // Bearer로 시작하는 JWT 형태를 의미한다.

    /*
        하나의 HTTP 요청당 이 필터는 딱 한 번만 실행되는 필터 함수이다.
        스프링 시큐리티의 전체 필터 체인 구조에서 우리가 원하는 필터를 하나 추가한 형태이다.
        즉 HTTP 요청 하나를 가로채서 인증 정보를 SecurityContext에 주입하고 다시 Spring Security 흐름으로 돌려보내는 함수이다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 요청 헤더의 authorization 키값 조회로 요청 헤더의 Authorization값을 가져온다. ex) Authorization: Bearer ABCsdfW...
            String authorizationHeader = request.getHeader(HEADER_AUTHORIZATION);

            // 토큰이 없는 경우 그냥 통과 시키고 세큐리티에서 처리하도록 한다.
            if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 가져온 값에서 접두사를 제거한다.
            String token = getAccessToken(authorizationHeader);

            if (tokenProvider.validToken(token)) {  // 토큰의 유효성을 검사한다.
                // 사용자 식별자, 권한, 인증 여부를 포함한 Authentication 객체를 생성한다.
                Authentication authentication = tokenProvider.getAuthentication(token);

                // 스프링 시큐리티에게 이 요청은 인증된 사용자라고 알려주는 핵심 단계이다.
                // 이후 @PreAuthorize, authenticated() 등의 인증에 모두 정상 동작하게 된다.
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            } else {  // 토큰은 있지만 유효하지 않은 경우 401을 반환한다.
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        catch(Exception e){
            // [핵심] 에러가 발생하면 렌더 로그에 범인을 출력함
            System.out.println("🚨 [Filter Error Found]: " + e.getMessage());
            e.printStackTrace(); // 에러가 발생한 코드의 줄 번호까지 로그에 찍어줌

            // 에러가 나도 서버가 아예 죽지 않도록 다음 필터로 흐름을 넘김
            filterChain.doFilter(request, response);
            return;
        }
        // 다음 필터로 요청 전달한다.
        // 해당 클래스인 필터 체인은 스프링 시큐리티의 필터 체인에 끼워넣은 구조이기 때문이다.
        filterChain.doFilter(request, response);
    }

    /*
        실제로 헤더에서 접두사를 제거해주는 함수이다.
        매개변수로 헤더의 authorization값을 받아 접두사를 제거한 문자열을 반환한다.
     */
    private String getAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length() + 1);  // 접두사 bearer 제거한다.
        }
        return null;  // 문자열이 비어있거나 Bearer로 시작하지 않으면 null을 반환한다.
    }
}
