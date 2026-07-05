// 정하늘

package me.jsu.springbootdeveloper.challenge.exception;

/*
    초대 코드에 해당하는 ChallengeInviteLink를 찾지 못한 경우의 예외이다.
 */
public class ChallengeInviteLinkNotFoundException extends RuntimeException {
    private final String errorCode;

    public ChallengeInviteLinkNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
