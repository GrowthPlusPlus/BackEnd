// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    하루에 한 챌린지에 대해 중복 인증을 하는 경우의 예외처리 이다.
 */
public class DuplicateDailyPostException extends RuntimeException {
    private final String errorCode;

    public DuplicateDailyPostException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}