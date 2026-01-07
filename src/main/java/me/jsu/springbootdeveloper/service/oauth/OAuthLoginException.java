// 정성우

package me.jsu.springbootdeveloper.service.oauth;

/**
 * OAuth 로그인 과정에서 발생하는 인증 실패 전용 예외
 * (잘못된 code, ID Token 검증 실패 등)
 */
public class OAuthLoginException extends RuntimeException {

    private final String errorCode;

    public OAuthLoginException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}