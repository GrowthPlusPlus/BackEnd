// 정성우

package me.jsu.springbootdeveloper.rag.exception;

/*
    RAG 질의 요청에서 query가 비어있거나 유효하지 않은 경우 발생하는 예외 클래스이다.
 */
public class InvalidQueryException extends RuntimeException {

    private final String errorCode;

    public InvalidQueryException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}