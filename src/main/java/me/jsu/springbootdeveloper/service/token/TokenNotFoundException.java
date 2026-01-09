// 정성우

package me.jsu.springbootdeveloper.service.token;

/*
    DB에 refresh token 없음, 이미 삭제된 refresh token, 재사용 시도 등으로 토큰은 형식상 정상이나,
    서버 상태상 유효하지 않은 경우의 예외처리를 위한 클래스이다.
 */
public class TokenNotFoundException extends RuntimeException {
    private final String errorCode;

    public TokenNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
