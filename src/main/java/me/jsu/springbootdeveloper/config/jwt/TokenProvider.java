// 정성우

package me.jsu.springbootdeveloper.config.jwt;

import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

/*
    해당 클래스는 엑세스 토큰 또는 리프레시 토큰을 생성하는 클래스이다.
 */
@RequiredArgsConstructor  // final로 선언된 필드를 초기화하기 위한 생성자를 자동으로 만들어준다.
@Service  // 스프링 컨테이너에 빈으로 등록한다.
public class TokenProvider {
    private final JwtProperties jwtProperties;

    /*
        스프링부트 버전 업데이트 이후 보안을 위해 사용되는 함수이다.
        서버의 비밀키를 문자열 그대로 사용하는 것이 아니라 암호화하여 사용한다.
        시크릿 키를 이용하여 암호화된 키를 반환한다.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtProperties.getSecretKey());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /*
        토큰을 생성할때 호출하는 함수이다.
        토큰을 생성하는 유저 정보와 토큰의 만료 시간을 매개변수로 받는다.
        makeToken 함수에서 생성된 JWT 토큰 문자열을 반환한다.
     */
    public String generateToken(User user, Duration expiredAt) {
        Date now = new Date();

        // 현재시간 + 설정한 만료시간을 더해서 만료일을 구하고, 그 값과 유저 정보를 makeToken으로 전달한다.
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user);
    }

    /*
        jwt 토큰을 실제로 생성하는 함수이다.
        만료 시간과 유저 정보를 매개변수로 받는다.
        만료시간, 토큰 설정, 유저 정보 등을 바탕으로 JWT 토큰 문자열을 생성하여 반환한다.
     */
    private String makeToken(Date expiry, User user) {
        Date now = new Date();

        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)  //헤더 type: JWT
                // iss: jwt에서 설정한 값으로 토큰을 발급한 사람의 이메일이다. (예시: ajufresh@gmail.com)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)  // 토큰이 발급된 현재 시간 iat
                .setExpiration(expiry)  // 토큰 만료 시간
                .setSubject(user.getEmail())  // 토큰의 주인, 즉 토큰을 받는 유저의 이메일
                .claim("id", user.getId())  // 유저의 id
                .claim("role", user.getRole()) // ROLE_USER, ROLE_ADMIN 등의 유저 권한
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 서명: 비밀값과 함께 해시값을 HS256방식으로 암호화한다.
                .compact();  // 위 모든 사항을 반영하여 최종 JWT문자열을 생성한다.
    }

    /*
        jwt 토큰을 받아서 파싱해주고, 유효성 검사를 해주는 함수이다.
        토큰을 매개변수로 받아서 파싱한 값(payload)을 반환한다.
        JWT는 Header.Payload.Signature의 구조로 이루어져있고 해당 함수는 Payload(Claims)부분만 반환한다.
        payload에는 iss, sub, iat, exp 필드가 들어있다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()  // JWT를 검증할 때 사용하는 기본 엔트리 포인트로 토큰 해석, 서명검증, JSON파싱 등을 한다.
                .setSigningKey(getSigningKey())  // JWT 서명 검증에 사용할 비밀키를 설정한다.
                .build()
                .parseClaimsJws(token)  // JWT 형식, 서명, 만료 시간을 검증하고 Claims를 파싱한다.
                .getBody();  // 검증이 완료된 JWT의 Payload(Claims)를 반환한다.
    }

    /*
        jwt 토큰 유효성 검증 함수이다.
        parseClaims함수가 이미 유효성 검사를 해주지만 결과를 true, false로 반환받을 수 있다.
        매개변수로 토큰을 받아서 유효성 여부에 관한 boolean값을 반환한다.
     */
    public boolean validToken(String token) {
        if (token == null || token.trim().isEmpty()) {  // 토큰이 비어있는지 검사한다.
            return false;
        }

        try {
            parseClaims(token);  // 검증만 수행하므로 결과값은 사용하지 않는다.

            return true;
        } catch (JwtException | IllegalArgumentException e) {  // 토큰이 위조되거나 만료되거나 형식이 잘못된 경우엔 유효하지 않은 토큰이다.
            return false;
        }
    }

    /*
        토큰 기반으로 인증 정보를 가져오는 메서드이다.
        @PreAuthorize가 호출되기 전에 스프링 시큐리티가 자동으로 호출하여 검사하는 용도이다.
        현재는 권한이 하나밖에 없으므로 확장 가능성을 염두해둔 기능이다.
        토큰을 매개변수로 받아서 jwt인증이 완료된 사용자의 신원과 권한을 담은 객체를 반환한다.
        
        추후 @PreAuthorize("hasRole('ADMIN')")를 통해 권한 별로 기능을 제한할 수 있다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);  // JWT토큰을 파싱하여 클레임 객체로 변환한다. (이메일, 만료일 등의 정보 추출)
        String role = claims.get("role", String.class);  // jwt토큰에 있는 권한을 가져온다.

        // 권한이 1개인 사용자를 표현하는 Set이다. User 클래스의 role이 한 값만 가지므로 권한은 1개만 가진다.
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority(role));

        /*
            UsernamePasswordAuthenticationToken의 첫번째 인자는 인증된 사용자 정보를 보여준다.

            claims.getSubject()
            JWT의 sub 값으로 보통 email이다.
            즉 이 인증 사용자의 식별 값이다.

            ""
            비밀번호는 JWT 인증 흐름에서는 필요 없으므로 빈 문자열로 둔다.
            JWT 자체가 이미 인증 증명(token)이기 때문이다.

            authorities
            이 사용자가 가진 권한 목록이다.
            일반적으론 ROLE_USER 권한을 가진다.

            두번째 인자는 인증 증명 수단으로 여기선 jwt 토큰 문자열을 사용한다.

            세번째 인자는 최종적으로 스프링 시큐리티가 인가 판단 하는 권한 목록이다.
            첫번째 인자의 authorities는 참고하지 않는다.
            하지만 UsernamePasswordAuthenticationToken 생성 자체는 항상 성공한다.
         */
        return new UsernamePasswordAuthenticationToken(new org.springframework.security.core.userdetails.
                User(claims.getSubject(), "", authorities), token, authorities);
    }

    /*
        토큰 기반으로 유저 ID를 가져오는 함수이다.
        토큰을 매개변수로 받아서 파싱하고, 파싱해서 나온 claim에 있는 userId 정보를 반환한다.
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("id", Long.class);  // claims에 있는 id값을 Long타입으로 반환한다.
    }
}
