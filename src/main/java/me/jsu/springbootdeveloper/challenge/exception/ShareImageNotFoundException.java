// 정성우

package me.jsu.springbootdeveloper.challenge.exception;

/*
    공유 이미지를 찾을 수 없는 경우에 생기는 예외이다.
 */
public class ShareImageNotFoundException extends RuntimeException {
    private final String errorCode;

    public ShareImageNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
