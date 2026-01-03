package me.jsu.springbootdeveloper.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@RequiredArgsConstructor
@Service  // 스프링 컨테이너에 빈으로 등록
public class TokenProvider {  // 엑세스 or 리프레시 토큰을 생성하는 클래스

    private final JwtProperties jwtProperties;

    public String generateToken(User user, Duration expiredAt) {
        Date now = new Date();
        // 현재시간 + 설정한 만료시간을 더해서 만료일을 구하고, 그 값과 유저 정보를 makeToken으로 전달함
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user);  
    }

    //jwt 토큰 생성 메서드
    private String makeToken(Date expiry, User user) {
        Date now = new Date();

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)  //헤더 type: JWT
                // 내용 iss: jwt에서 설정한 값 -> ajufresh@gmail.com (토큰을 발급한 사람의 이메일)
                .setIssuer(jwtProperties.getIssuers())
                .setIssuedAt(now)  // 토큰이 발급된 현재 시간 iat
                .setExpiration(expiry)  // expiry 맴버 변수 exp -> 인자로 받은 만료시간을 토큰에 설정
                .setSubject(user.getEmail())  // 토큰을 받는 유저의 이메일 -> 내용 sub
                .claim("id", user.getId())  // 유저의 id -> 클레임 id
                // 서명: 비밀값과 함께 해시값을 HS256방식으로 암호화
                .signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
                .compact();  // 위 모든 사항을 반영하여 최종 JWT문자열을 생성해 반환해줌
    }

    // jwt 토큰 유효성 검증 메서드 -> 빠르게 유효한지만 검사
    public boolean validToken(String token) {
        try {
            Jwts.parser()  // JWT를 검증할 때 사용하는 기본 엔트리 포인트 -> 토큰 해석, 서명검증, JSON파싱 등
                    .setSigningKey(jwtProperties.getSecretKey())  // 비밀키 값으로 복호화 -> 검증할 키를 준비하는 과정 (서버가 가지고 있는 비밀키)
                    .parseClaimsJws(token);  // 비밀 서버키와 토큰을 실제로 검증하는 부분

            return true;
        } catch (Exception e) {  // 복호화 과정에서 오류가 나면 유효하지 않은 토큰
            return false;
        }
    }

    // 토큰 기반으로 인증 정보를 가져오는 메서드
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);  // JWT토큰에 들어있는 xxxx.yyyy.zzzz문자열을 파싱하여 클레임 객체로 변환함 (이메일, 만료일 등의 정보만 추출) + 유효성 검사
        /*
        Spring Security는 권한을 GrantedAuthority 로 관리한다.
        여기서는 사용자에게 ROLE_USER 라는 권한 1개를 부여하고 있다.
        Collections.singleton() → 1개의 요소만 가진 불변 Set 생성
        프로젝트가 단일 구조이기 때문에 회원권한 하나만 지정함 -> 스프링 세큐리티에 필요한 GrantedAuthority 객체 생성
        원래대로라면 JWT토큰에 있는 권한을 해석해서 권한 목록에 넣어야 하지만 여기선 권한이 하나만 있다고 가정하고 ROLE_USER만 넣음
         */
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        /*
        claims.getSubject()
        JWT의 sub 값 → 보통 email 또는 username
        → “이 인증 사용자의 식별 값”

        ""
        비밀번호는 JWT 인증 흐름에서는 필요 없으므로 빈 문자열
        JWT 자체가 이미 인증 증명(token)이기 때문이다.

        authorities
        이 사용자가 가진 권한 목록
        (현재 예제에서는 ROLE_USER 1개)

        첫번째 인자의 권한은 사용자 원래 가진 권한을 보여주는 용도이고 세번째 인자의 권한이 실제로 권한을 판단하는데 사용되는 인자이다.
         */
        return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities), token, authorities);
    }

    // 토큰 기반으로 유저 ID를 가져오는 메서드
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);  // claims에 있는 id값을 Long타입으로 반환
    }

    // 토큰을 필요한 부분만 추출하는 용도
    private Claims getClaims(String token) { 
        return Jwts.parser()  // 유효성 검사와 중복 부분으로 수정 가능
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody();  // Claim 클래스 형태로 반환해줌
    }
}
