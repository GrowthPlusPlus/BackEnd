// 정성우

package me.jsu.springbootdeveloper.controller.error;

import me.jsu.springbootdeveloper.dto.error.ErrorResponse;
import me.jsu.springbootdeveloper.service.oauth.OAuthLoginException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OAuthLoginException.class)
    public ResponseEntity<ErrorResponse> handleOAuthLoginFail(
            OAuthLoginException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        "LOGIN_FAILED",
                        e.getErrorCode()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleServerError() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
    }
}