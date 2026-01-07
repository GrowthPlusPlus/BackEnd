// 정성우

package me.jsu.springbootdeveloper.service.oauth;

/*
    OAuth 로그인 과정에서 발생하는 인증 실패 전용 예외 클래스로, 글로벌 핸들러가 catch하는 조건이다.
    따라서 글로벌 핸들러는 잘못된 code, ID Token 검증 실패 등을 잡는다.
    즉 OAuthLoginException.class 역할이고 실제 오류 처리는 글로벌 핸들러에서 수행한다.

    여기서 발생하는 예외는 프로그램 버그가 아닌 로그인 유스케이스 중 발생할 수 있는 정상적인 실패 상황을 나타낸다.
 */
public class OAuthLoginException extends RuntimeException {  // throws 선언 없이 글로벌 에러 핸들러로 전달한다.
    private final String errorCode;  // OAuth 로그인 실패의 원인을 식별하기 위한 코드이다. (여기선 INVALID_AUTH_CODE 이다.)

    public OAuthLoginException(String errorCode) {
        super(errorCode);  // RuntimeException의 메시지(message)로 errorCode를 설정한다. 사람을 위한 메세지 값이다.
        /*
            예외 객체 내부에 실패 원인 코드를 명시적으로 저장한다.
            글로벌 에러 핸들러에서 getErrorCode()로 추출 가능하다.
            시스템과 앱을 위한 값이다.
         */
        this.errorCode = errorCode;
    }

    // 예외 발생 시 지정한 실패 원인 코드를 반환한다.
    public String getErrorCode() {
        return errorCode;
    }
}