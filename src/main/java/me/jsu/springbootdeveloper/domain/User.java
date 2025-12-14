package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class User implements UserDetails {  //userDetails를 상속 받아서 인증 객체로 사용함

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String password;

    @Builder
    public User(String email, String password, String auth, String nickname) {
        this.email = email;
        this.password = password;

        //OAuth를 위한 닉네임 추가
        this.nickname = nickname;
    }

    /*
    GrantedAuthority는 ROLE 또는 권한 문자열 하나를 저장하는 인터페이스다.
    SimpleGrantedAuthority로 user라는 권한을 가지는 정보 객체를 생성한다.
    List.of로 준 이유는 권한이 여러개일 수 있기 때문이다.
     */

    @Override  //사용 권한 -> 유저이므로 유저 권한을 반환함
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("user"));  // 스프링 시큐리티에게 “이 유저가 가진 권한이 무엇인지” 알려주는 메서드
    }

    /*
    스프링 시큐리티는 엔티티의 Getter(@Getter) 메서드를 사용하지 않습니다.

    스프링 시큐리티가 비밀번호와 사용자명을 인식하는 기준은
    **반드시 UserDetails의 getUsername(), getPassword()** (Spring Security 인증용 (공식 필수 메서드)) 입니다.
     */

    // 사용자의 고유한 id를 반환함
    @Override
    public String getUsername() {
        return email;
    }

    // 사용자의 패스워드 반환
    @Override
    public String getPassword() {
        return password;
    }

    /*
    1. 아직 계정 상태 체크 로직을 사용하지 않는다.
    2. 계정은 모두 정상(active)이라고 가정한다.
    3. 관리 기능이 없다면 true를 반환하는 것이 가장 일반적이다.
     */

    // 계정 만료 여부 반환
    @Override
    public boolean isAccountNonExpired() {
        // 만료되었는지 확인하는 로직
        return true;  // true -> 만료되지 않음
    }

    // 계정 잠금 여부 반환
    @Override
    public boolean isAccountNonLocked() {
        // 계정 잠금되었는지 확인하는 로직
        return true;  // true -> 잠금되지 않음
    }

    // 패스워드 만료 여부 반환
    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // true -> 만료되지 않음
    }

    // 계정 사용 가능 여부 반환
    @Override
    public boolean isEnabled() {
        // 계정이 아직 사용 가능한지 확인하는 로직
        return true;  // true - > 사용 가능
    }

    // OAuth 관련 코드

    // 사용자 이름
    @Column(name = "nickname", unique = true)
    private String nickname;

    // 사용자 이름 변경
    public User update(String nickname) {
        this.nickname = nickname;

        return this;
    }
}
