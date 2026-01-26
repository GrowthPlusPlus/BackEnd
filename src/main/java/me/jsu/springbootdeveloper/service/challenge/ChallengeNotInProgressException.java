// 정성우

package me.jsu.springbootdeveloper.service.challenge;

/*
    현재 유저의 챌린지 상태가 진행중이 아닐 때 발생하는 예외처리이다.
 */
public class ChallengeNotInProgressException extends RuntimeException {
    private final String errorCode;

    public ChallengeNotInProgressException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}