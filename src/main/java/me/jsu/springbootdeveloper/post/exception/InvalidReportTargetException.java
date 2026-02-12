// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    객체와 신고내역의 객체 id가 동일하지 않은 경우의 예외처리를 위한 클래스이다.
 */
public class InvalidReportTargetException extends RuntimeException {
    private final String errorCode;

    public InvalidReportTargetException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
