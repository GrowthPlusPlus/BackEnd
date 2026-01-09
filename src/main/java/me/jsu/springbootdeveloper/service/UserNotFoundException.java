// 정성우

package me.jsu.springbootdeveloper.service;

/*
    DB에서 유저 정보를 검색하지 못한 경우에 실행하는 예외처리 클래스이다.
 */
public class UserNotFoundException extends RuntimeException {
    private final String errorCode;

    public UserNotFoundException(String errorCode) {
        super(errorCode);   // 사람을 위한 메시지
        this.errorCode = errorCode;  // 시스템/클라이언트 분기용 코드
    }

    public String getErrorCode() {
        return errorCode;
    }
}
