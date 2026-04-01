// 정성우

package me.jsu.springbootdeveloper.user.exception;

/*
    올바르지 않는 년도가 들어온 경우의 예외처리를 위한 클래스이다.
 */
public class InvalidYearException extends RuntimeException {
    private final String errorCode;

    public InvalidYearException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
