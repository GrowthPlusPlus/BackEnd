// 정성우

package me.jsu.springbootdeveloper.rag.exception;

/*
    업로드 하려는 문서가 형식에 맞지 않는 경우의 예외 클래스이다.
 */
public class InvalidDocumentFormatException extends RuntimeException {

    private final String errorCode;

    public InvalidDocumentFormatException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}