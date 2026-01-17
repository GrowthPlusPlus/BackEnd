// 정성우

package me.jsu.springbootdeveloper.service;

/*
    DB에 이미 중복된 닉네임을 가진 유저가 존재하는 경우 발생한다.
 */
public class DuplicateNicknameException extends RuntimeException {
    private final String errorCode;

    public DuplicateNicknameException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
