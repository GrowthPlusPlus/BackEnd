// 정성우

package me.jsu.springbootdeveloper.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*
    DB에 저장될 유저 정보를 저장하는 클래스이다.
 */
@Table(
        name = "user",
        indexes = {
                @Index(  // 유저 이메일은 서버에 단 하나로 유니크하게 취급하며, 이메일 검색을 인덱싱 한다.
                        name = "uk_user_email",
                        columnList = "email",
                        unique = true
                )
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class User implements UserDetails {  // userDetails를 상속 받아서 인증 객체로 사용한다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")  // OAuth 사용자는 사용하지 않는다.
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;  // 유저 별 권한 지정을 위한 enum이다.

    @Column(name = "nickname", unique = true)
    private String nickname;  // 사용자의 이름이다.

    @CreatedDate  // 유저가 생성된 시간이다.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String email, String password, Role role, String nickname) {
        this.email = email;
        this.password = password;
        this.role = role;  // 회원가입 시 role(권한)이 추가된다.
        this.nickname = nickname;  //OAuth 로그인 시, 구글 프로필에 사용된 이름이 저장된다.
    }

    /*
        사용자 이름을 변경해주는 함수로, OAuth 사용 시 최초 로그인에는 무조건 OAuth 프로필의 닉네임이 들어가므로,
        닉네임을 수정하고 싶다면 해당 함수를 사용해야 한다.
    */
    public User update(String nickname) {
        this.nickname = nickname;

        return this;
    }

    /*
        GrantedAuthority는 ROLE 또는 권한 문자열 하나를 저장하는 인터페이스다.
        SimpleGrantedAuthority로 user라는 권한을 가지는 정보 객체를 생성한다.
        List.of로 준 이유는 스프링 시큐리티는 권한이 여러 개일 수 있다는 전제 설계되었기 때문에 단일 권한을 갖더라도 컬렉션으로 반환한다.

        기본적으로 유저는 ROLE_USER 권한을 갖는다.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role.name()));  // 스프링 시큐리티에게 이 유저가 가진 권한이 무엇인지 알려주는 함수이다.
    }

    /*
        스프링 시큐리티는 보안을 위해 엔티티의 @Getter 함수를 사용하지 않는다.
        스프링 시큐리티가 비밀번호와 사용자명을 인식하는 기준은 오직 UserDetails의 인터페이스의 함수만 사용한다.
        즉, UserDetails의 getUsername(), getPassword() (Spring Security 인증용 함수)를 유저가 직접 override하여 구현한 함수를 쓴다.

        OAuth 로그인에서는 사용하지 않고, 우리 서비스에 직접 회원가입한 유저의 경우에만 사용하는 인증이다.
        단, 로그인에선 사용하지 않지만, PostApiController처럼 게시글을 올리는 경우 스프링 시큐리티가 사용하기도 한다.
     */
    @Override
    public String getUsername() {
        return email;
    }
    @Override
    public String getPassword() {
        return password;
    }

    /*
        1. 아직 계정 상태 체크 로직을 사용하지 않는다.
        2. 계정은 모두 정상(active)이라고 가정한다.
        3. 관리 기능이 없다면 true를 반환하는 것이 가장 일반적이다.
     */
    @Override
    public boolean isAccountNonExpired() {   // 계정 만료 여부를 반환하는 함수이다.
        // 만료되었는지 확인하는 로직
        return true;  // true면 만료되지 않았음을 의미한다.
    }
    @Override
    public boolean isAccountNonLocked() {  // 계정 잠금 여부를 반환하는 함수이다.
        // 계정 잠금되었는지 확인하는 로직
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {  // 패스워드 만료 여부를 반환하는 함수이다.
        return true;
    }
    @Override
    public boolean isEnabled() {  // 계정 사용 가능 여부를 반환하는 함수이다.
        // 계정이 아직 사용 가능한지 확인하는 로직
        return true;
    }
}
