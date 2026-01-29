// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    챌린지 방장이 챌린지에서 나가기를 시도할 때 발생하는 예외이다.
 */
public class ChallengeOwnerCannotLeaveException extends RuntimeException {
    private final String errorCode;

    public ChallengeOwnerCannotLeaveException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
