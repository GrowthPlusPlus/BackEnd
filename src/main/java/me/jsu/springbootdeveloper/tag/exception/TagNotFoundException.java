// 정성우

package me.jsu.springbootdeveloper.tag.exception;

/*
    태그를 DB에서 찾지 못했을 경우를 위한 예외 클래스이다.
 */
public class TagNotFoundException extends IllegalArgumentException {
    private final String errorCode;

    public TagNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
