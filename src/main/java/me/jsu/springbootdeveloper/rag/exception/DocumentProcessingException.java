// 정성우

package me.jsu.springbootdeveloper.rag.exception;

/*
    문서 처리 중 서버 내부 오류가 발생한 경우에 대한 예외 클래스이다.
 */
public class DocumentProcessingException extends RuntimeException {

    private final String errorCode;

    public DocumentProcessingException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public DocumentProcessingException(String errorCode, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
