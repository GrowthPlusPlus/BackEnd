// 정성우

package me.jsu.springbootdeveloper.user.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import org.springframework.stereotype.Component;

/*
    닉네임 중복 생성 방지를 위해 무작위로 만들어주는 클래스이다.
 */
@Component
@RequiredArgsConstructor
public class NicknameGenerator {
    private final UserRepository userRepository;

    public String generateNickname() {
        String nickname;
        do {
            // 랜덤 닉네임을 생성한다.
            nickname = "user_" + UUID.randomUUID().toString().substring(0, 8);

        } while (userRepository.existsByNickname(nickname));  // DB에 동일 닉네임이 존재하면 다시 생성한다.

        return nickname;
    }
}
