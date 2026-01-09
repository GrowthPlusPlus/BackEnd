// 정성우

package me.jsu.springbootdeveloper.service;

/*
    게시글을 찾지 못한 경우의 예외처리를 위한 클래스이다.
 */
public class ArticleNotFoundException extends RuntimeException {
    private final String errorCode;

    public ArticleNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
