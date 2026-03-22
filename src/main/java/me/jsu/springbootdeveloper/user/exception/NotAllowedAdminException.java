// 정성우

package me.jsu.springbootdeveloper.user.exception;

/*
    어드민으로써 수행하면 안되는 행위를 한 경우 발생한다.
 */
public class NotAllowedAdminException extends RuntimeException {
    private final String errorCode;

    public NotAllowedAdminException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}