// 정성우

package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import lombok.Setter;

/*
    우리 서비스 자체에서 회원가입을 할 때 필요한 정보를 가진 클래스이다.
    OAuth 로그인 방식에서는 사용되지 않는다.
 */
@Getter
@Setter
public class AddUserRequest {
    private String email;
    private String password;
}
