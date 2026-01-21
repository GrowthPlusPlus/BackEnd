package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.domain.user.User;

/*
    해당 클래스는 유저 정보를 반환할 때 사용할 DTO이다.
    비밀번호와 같은 민감한 정보는 제외하고 전송한다.
    최초 작성자: 여하늘
 */
@Getter
public class UserResponse {

    private final Long id;
    private final String email;
    private final String nickname;
    private final String role;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.role = user.getRole().name(); // Enum을 String으로 변환

    }

}



