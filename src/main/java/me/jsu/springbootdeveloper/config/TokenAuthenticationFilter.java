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

@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final static String HEADER_AUTHORIZAION = "Authorization";  // 요청 해더의 키 이름
    private final static String TOKEN_PREFIX = "Bearer";  // Bearer로 시작하는 JWT 형태를 의미함

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 요청 헤더의 authorization 키값 조회
        String authorizationHeader = request.getHeader(HEADER_AUTHORIZAION);  // 요청 헤더의 Authorization값을 가져옴 ex) Authorization: Bearer ABCsdfW...
        // 가져온 값에서 접두사 제거
        String token = getAccessToken(authorizationHeader);
        // 가져온 토큰이 유효한지 확인하고 유효하면 인증 정보 설정
        if (tokenProvider.validToken(token)) {
            Authentication authentication = tokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);  //스프링 시큐리티에게 이 요청은 인증된 사용자라고 알려줌 -> 핵심단계
        }

        filterChain.doFilter(request, response);  // 다음 필더로 요청 전달
    }

    private String getAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            return authorizationHeader.substring(TOKEN_PREFIX.length());  // 접두사 bearer 제거
        }
        return null;
    }
}
