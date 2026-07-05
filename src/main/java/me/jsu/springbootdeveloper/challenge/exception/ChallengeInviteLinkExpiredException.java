// 정하늘

package me.jsu.springbootdeveloper.challenge.exception;

/*
    초대 링크가 만료되었거나 더 이상 사용할 수 없을 때 발생하는 예외이다.
 */
public class ChallengeInviteLinkExpiredException extends RuntimeException {
    private final String errorCode;

    public ChallengeInviteLinkExpiredException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
