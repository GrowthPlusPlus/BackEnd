// 정성우

package me.jsu.springbootdeveloper.controller.error;

import me.jsu.springbootdeveloper.dto.error.ErrorResponse;
import me.jsu.springbootdeveloper.service.oauth.OAuthLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
    해당 클래스의 @RestControllerAdvice는 컨트롤러에서 try-catch 구문을 두지 않아도 예외를 처리할 수 있도록 해준다.
    따라서 오류가 발생한 경우, 모바일 앱에 예외 유형, 예외 원인을 json 형태로 전달하여 처리할 수 있도록 한다.
    앱/비즈니스 흐름용의 try-catch 구문을 대체하는 역할을 하며, 복구, 분기, 자원 관리 목적의 try-catch문은 그대로 둬야 한다.

    스프링은 가장 구체적인 예외 핸들러를 우선 적용한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
        OAuth 로그인을 실패하는 경우의 핸들러이다.
        요청 처리 중 OAuthLoginException 타입 예외가 발생하면 이 함수가 실행된다.
        즉 이 타입의 예외를 처리할 담당자이다.
     */
    @ExceptionHandler(OAuthLoginException.class)
    public ResponseEntity<ErrorResponse> handleOAuthLoginFail(
            OAuthLoginException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)  // http 상태 코드가 401 Unauthorized이다.
                .body(new ErrorResponse(
                        "LOGIN_FAILED",  // 로그인 실패 유형이다.
                        e.getErrorCode()  // 로그인 실패에 대한 구체적인 실패 원인 코드이다. (INVALID_ID_TOKEN, INVALID_AUTH_CODE 등)
                ));
    }

    /*
        위에서 정의하지 않은 모든 기타 서버 오류들을 처리해준다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleServerError() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // http 상태 코드가 500이다.
                .build();  // 내부 구현 세부 정보 노출을 방지하기 위해 따로 body를 두지 않는다.
    }
}