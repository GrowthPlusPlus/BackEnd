// 정성우

package me.jsu.springbootdeveloper.token.exception;

/*
    토큰 서명 위조, 토큰 만료, 형식 오류 등으로 인증 자체가 실패한 경우에 대한 예외 처리 클래스이다.
 */
public class InvalidTokenException extends RuntimeException {
    private final String errorCode;

    public InvalidTokenException(String errorCode) {
        super(errorCode);  // 사람을 위한 메시지
        this.errorCode = errorCode;  // 시스템/앱을 위한 코드
    }

    public String getErrorCode() {
        return errorCode;
    }
}