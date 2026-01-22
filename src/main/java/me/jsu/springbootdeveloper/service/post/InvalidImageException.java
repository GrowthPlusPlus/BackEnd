// 정성우

package me.jsu.springbootdeveloper.service.post;

/*
    클라이언트가 잘못된 이미지 요청을 했을 경우의 예외처리이다.
 */
public class InvalidImageException extends RuntimeException {
    private final String errorCode;

    public InvalidImageException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
