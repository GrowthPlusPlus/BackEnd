// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    인증글 좋아요를 찾지 못한 경우의 예외처리를 위한 클래스이다.
 */
public class PostLikeNotFoundException extends RuntimeException {
    private final String errorCode;

    public PostLikeNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}