// 정성우

package me.jsu.springbootdeveloper.service.post;

/*
    댓글을 찾지 못한 경우의 예외처리이다.
 */
public class CommentNotFoundException extends RuntimeException {
    private final String errorCode;

    public CommentNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
