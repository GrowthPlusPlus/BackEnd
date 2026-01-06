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
}
