// 정성우

package me.jsu.springbootdeveloper.service.post;

/*
    게시글 작성자와 요청자의 이메일이 달라 요청이 실패한 경우에 관한 예외처리 클래스이다.
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
