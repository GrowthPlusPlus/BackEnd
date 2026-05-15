// 정성우

package me.jsu.springbootdeveloper.admin.exception;

/*
    관리자 로그인/회원가입 중, 401 예외를 담당하는 클래스이다.
 */
public class AdminAuthUnauthorizedException extends AdminAuthException {
    public AdminAuthUnauthorizedException(String errorCode) {
        super(errorCode);
    }
}