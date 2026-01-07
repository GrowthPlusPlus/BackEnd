// 정성우 수정 예정

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.AddUserRequest;
import me.jsu.springbootdeveloper.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import static me.jsu.springbootdeveloper.domain.Role.ROLE_USER;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;


    // 회원가입 구현
    public Long save(AddUserRequest dto) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        return userRepository.save(User.builder()
                .email(dto.getEmail())
                //패스워드 암호화
                .password(encoder.encode(dto.getPassword()))
                .role(ROLE_USER)
                .build()).getId();  // 데베에 등록된 id를 리턴값으로 받음
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unexpected user"));
    }

    /**
     * 이메일로 유저를 찾거나 없으면 생성하는 함수
     * PKCE OAuth2 인증에서 사용자 정보를 처리하기 위해 사용한다.
     * 기존 유저가 있으면 닉네임을 업데이트하고, 없으면 새로 생성한다.
     * 
     * @param email 유저 이메일
     * @param name 유저 닉네임
     * @return 찾거나 생성된 User 엔티티
     */
    public User findOrCreate(String email, String name) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    // 필요하면 이름 갱신
                    user.update(name);
                    return userRepository.save(user);  // 업데이트 후 저장
                })
                .orElseGet(() ->
                        userRepository.save(
                                User.builder()
                                        .email(email)
                                        .nickname(name)
                                        .role(ROLE_USER)
                                        .build()
                        )
                );
    }
}
