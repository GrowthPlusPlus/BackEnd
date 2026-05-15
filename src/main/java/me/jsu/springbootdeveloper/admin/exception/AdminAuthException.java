// 정성우

package me.jsu.springbootdeveloper.admin.exception;

/*
    관리자용 로그인/회원가입을 담당하는 공통 부모의 exception 클래스이다.
 */
public class AdminAuthException extends RuntimeException {
    private final String errorCode;

    public AdminAuthException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
