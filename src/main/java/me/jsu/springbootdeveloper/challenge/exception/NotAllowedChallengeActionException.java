// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    챌린지에 관련하여 http status가 403인 비즈니스 예외를 처리하는 클래스이다.
 */
public class NotAllowedChallengeActionException extends RuntimeException {
    private final String errorCode;

    public NotAllowedChallengeActionException(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
