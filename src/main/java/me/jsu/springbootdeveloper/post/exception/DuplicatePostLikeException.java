// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    같은 게시글에 여러번 좋아요를 누른 경우의 예외처리이다.
 */
public class DuplicatePostLikeException extends RuntimeException {
    private final String errorCode;

    public DuplicatePostLikeException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}