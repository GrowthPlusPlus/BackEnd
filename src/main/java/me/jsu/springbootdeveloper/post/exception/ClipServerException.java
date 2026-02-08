// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    CLIP 서버의 응답이 비어있는 경우에 관한 예외처리 클래스이다.
 */
public class ClipServerException extends RuntimeException {
    private final String errorCode;

    public ClipServerException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}