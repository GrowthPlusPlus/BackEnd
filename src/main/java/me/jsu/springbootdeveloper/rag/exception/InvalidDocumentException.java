// 정성우

package me.jsu.springbootdeveloper.rag.exception;

/*
    문서 id가 존재하지 않는 경우의 예외 클래스이다.
 */
public class InvalidDocumentException extends RuntimeException {

    private final String errorCode;

    public InvalidDocumentException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
