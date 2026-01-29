// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    챌린지를 찾지 못한 경우의 예외처리를 위한 클래스이다.
 */
public class ChallengeNotFoundException extends RuntimeException {
    private final String errorCode;

    public ChallengeNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}