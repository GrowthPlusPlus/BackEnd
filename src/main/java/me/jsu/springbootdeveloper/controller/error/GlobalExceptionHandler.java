// 정성우

package me.jsu.springbootdeveloper.controller.error;

import me.jsu.springbootdeveloper.dto.error.ErrorResponse;
import me.jsu.springbootdeveloper.service.ArticleAccessDeniedException;
import me.jsu.springbootdeveloper.service.ArticleNotFoundException;
import me.jsu.springbootdeveloper.service.ChallengeNotFoundException;
import me.jsu.springbootdeveloper.service.UserNotFoundException;
import me.jsu.springbootdeveloper.service.oauth.OAuthLoginException;
import me.jsu.springbootdeveloper.service.token.InvalidTokenException;
import me.jsu.springbootdeveloper.service.token.TokenNotFoundException;
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
        토큰 인증 자체가 실패하는 경우의 핸들러이다.
     */
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "INVALID_TOKEN",
                        e.getErrorCode()
                ));
    }

    /*
        DB에서 토큰 검색에 실패하는 경우의 핸들러이다.
     */
    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTokenNotFound(
            TokenNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "TOKEN_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        DB에서 유저 검색에 실패하는 경우의 핸들러이다.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "USER_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        DB에서 게시글 검색에 실패하는 경우의 핸들러이다.
     */
    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleArticleNotFound(
            ArticleNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "ARTICLE_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        올바르지 않은 유저가 게시글 수정/삭제를 시도하는 경우의 핸들러이다.
     */
    @ExceptionHandler(ArticleAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleArticleAccessDenied(
            ArticleAccessDeniedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "ARTICLE_ACCESS_DENIED",
                        e.getErrorCode()
                ));
    }

    /*
        DB에서 챌린지를 검색하지 못한 경우의 핸들러이다.
    */
    @ExceptionHandler(ChallengeNotFoundException.class)
    public ResponseEntity<ErrorResponse>  handleArticleNotFound(
            ChallengeNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "CHALLENGE_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        위에서 정의하지 않은 모든 기타 서버 오류들을 처리해준다.
        spring 기본 처리에 예외를 맡기는 것이 좋다. 따라서 비활성화 한다.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleServerError() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // http 상태 코드가 500이다.
                .build();  // 내부 구현 세부 정보 노출을 방지하기 위해 따로 body를 두지 않는다.
    }
     */
}