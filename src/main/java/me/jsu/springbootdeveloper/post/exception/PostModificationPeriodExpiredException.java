// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    하루가 지나고 인증글을 업로드하는 경우 발생하는 예외이다.
 */
public class PostModificationPeriodExpiredException extends RuntimeException {
    private final String errorCode;

    public PostModificationPeriodExpiredException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
