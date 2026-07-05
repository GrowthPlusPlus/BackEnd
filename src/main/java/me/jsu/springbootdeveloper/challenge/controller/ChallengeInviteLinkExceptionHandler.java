// 정하늘

package me.jsu.springbootdeveloper.challenge.controller;

import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteLinkExpiredException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteLinkNotFoundException;
import me.jsu.springbootdeveloper.common.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
    초대 링크 resolve API 전용 예외 처리기이다.
    기존 GlobalExceptionHandler는 수정하지 않는다.
 */
@RestControllerAdvice(assignableTypes = ChallengeInviteLinkApiController.class)
public class ChallengeInviteLinkExceptionHandler {

    /*
        inviteCode에 해당하는 초대 링크를 찾지 못한 경우 404를 반환한다.
     */
    @ExceptionHandler(ChallengeInviteLinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInviteLinkNotFound(
            ChallengeInviteLinkNotFoundException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "INVITE_LINK_NOT_FOUND",
                        exception.getErrorCode()
                ));
    }

    /*
        만료되었거나 비활성화된 초대 링크인 경우 410 Gone을 반환한다.
     */
    @ExceptionHandler(ChallengeInviteLinkExpiredException.class)
    public ResponseEntity<ErrorResponse> handleInviteLinkExpired(
            ChallengeInviteLinkExpiredException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(new ErrorResponse(
                        "INVITE_LINK_EXPIRED",
                        exception.getErrorCode()
                ));
    }
}
