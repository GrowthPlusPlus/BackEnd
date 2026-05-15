// 정성우

package me.jsu.springbootdeveloper.admin.dto;

import lombok.Getter;

/*
    관리자로 로그인하는 dto이다. 다른 관리자에 의해 승인이 된 경우에만 로그인된다.
 */
@Getter
public class AdminLoginRequest {
    public String email;
    public String password;
    public String fcmToken;
}
