// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    신고를 찾지 못한 경우의 예외처리를 위한 클래스이다.
 */
public class ReportNotFoundException extends RuntimeException {
    private final String errorCode;

    public ReportNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}