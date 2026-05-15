// 정성우

package me.jsu.springbootdeveloper.user.exception;

/*
    DB에 이미 중복된 이메일을 가진 유저가 존재하는 경우 발생한다.
 */
public class DuplicateEmailException extends RuntimeException {
    private final String errorCode;

    public DuplicateEmailException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

