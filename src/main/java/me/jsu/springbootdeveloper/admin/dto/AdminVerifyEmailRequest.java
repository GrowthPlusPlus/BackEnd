// 정성우

package me.jsu.springbootdeveloper.admin.dto;

import lombok.Getter;

/*
    관리자 회원가입 시, 유저가 자신의 이메일로 온 인증 번호를 보낸다.
 */
@Getter
public class AdminVerifyEmailRequest {
    public String email;
    public String code;
}
