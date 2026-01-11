// 정성우

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.AddUserRequest;
import me.jsu.springbootdeveloper.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static me.jsu.springbootdeveloper.domain.Role.ROLE_USER;

/*
    해당 클래스는 유저 데이터베이스를 이용하여 검색하거나 데이터베이스에 엔트리를 저장하는 역할을 한다.
 */
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;

    /*
        회원가입을 구현한 함수이다. 현재는 OAuth 인증 방식을 사용하므로 사용되지 않는다.
     */
    public Long save(AddUserRequest dto) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        return userRepository.save(User.builder()
                .email(dto.getEmail())
                .password(encoder.encode(dto.getPassword()))  // 패스워드를 암호화한다.
                .role(ROLE_USER)
                .build()).getId();  // 데베에 등록된 id를 리턴값으로 반환한다.
    }

    /*
        유저 id를 기반으로 유저 정보를 검색하는 함수이다.
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_ID"));
    }

    /*
        유저 이메일을 기반으로 유저 정보를 검색하는 함수이다.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_EMAIL"));
    }

    /*
        이메일로 유저를 찾거나 없으면 생성하는 함수이다.
        PKCE OAuth2 인증에서 사용자 정보를 처리하기 위해 사용한다.
        기존 유저가 있으면 닉네임을 업데이트하고, 없으면 새로 생성한다.

        @param email 유저 이메일
        @param name 유저 닉네임
        @return 찾거나 생성된 User 엔티티
     */
    public User findOrCreate(String email, String name) {
        return userRepository.findByEmail(email)
                // .map(user -> user) 기존 유저가 존재하는 경우이다. 여기선 사용하지 않으므로 주석처리한다.
                .orElseGet(() ->  // 신규 유저인 경우이다.
                        userRepository.save(
                                User.builder()
                                        .email(email)
                                        .nickname(name)
                                        .role(ROLE_USER)  // 신규 유저는 일반 유저로 권한을 부여한다.
                                        .build()
                        )
                );
    }

    /*
        유저 닉네임을 기반으로 유저 정보를 검색하는 함수이다。
     */
    public User findByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_NICKNAME"));
    }
}
