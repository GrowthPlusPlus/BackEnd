// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    챌린지 초대를 찾지 못한 경우의 예외처리를 위한 클래스이다.
 */
public class ChallengeInviteNotFoundException extends RuntimeException {
    private final String errorCode;

    public ChallengeInviteNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}