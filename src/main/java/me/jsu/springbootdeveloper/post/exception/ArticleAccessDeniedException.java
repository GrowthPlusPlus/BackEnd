// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    인증글에 관해 허용되지 않은 접근을 하는 경우에 관한 예외처리 클래스이다.
 */
public class ArticleAccessDeniedException extends RuntimeException {
    private final String errorCode;

    public ArticleAccessDeniedException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
