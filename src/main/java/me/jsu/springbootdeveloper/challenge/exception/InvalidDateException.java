// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    존재할 수 없는 연, 월이 들어온 경우 예외처리 한다.
 */
public class InvalidDateException extends RuntimeException {
    private final String errorCode;

    public InvalidDateException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
