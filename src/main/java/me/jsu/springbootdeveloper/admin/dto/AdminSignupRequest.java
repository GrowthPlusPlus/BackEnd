// 정성우

package me.jsu.springbootdeveloper.admin.dto;

import lombok.Getter;

/*
    관리자 권한으로 회원가입을 요청하는 dto이다.
    해당 유저의 이메일로 인증번호를 보낸다.
 */
@Getter
public class AdminSignupRequest {
    public String email;
    public String password;
    public String passwordVerify;
    public String nickName;
}
