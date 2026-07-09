// 정성우

package me.jsu.springbootdeveloper.common;

import me.jsu.springbootdeveloper.admin.exception.*;
import me.jsu.springbootdeveloper.challenge.exception.*;
import me.jsu.springbootdeveloper.post.exception.*;
import me.jsu.springbootdeveloper.cloudinary.exception.CloudinaryDeleteException;
import me.jsu.springbootdeveloper.cloudinary.exception.CloudinaryUploadException;
import me.jsu.springbootdeveloper.oauth.exception.OAuthLoginException;
import me.jsu.springbootdeveloper.rag.exception.*;
import me.jsu.springbootdeveloper.tag.exception.TagNotFoundException;
import me.jsu.springbootdeveloper.token.exception.InvalidTokenException;
import me.jsu.springbootdeveloper.token.exception.TokenNotFoundException;
import me.jsu.springbootdeveloper.user.exception.*;
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
    public ResponseEntity<ErrorResponse>  handleChallengeNotFound(
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
        클라우디너리에 사진을 업로드하지 못한 경우의 핸들러이다.
    */
    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ErrorResponse>  handleCloudinaryError(
            CloudinaryUploadException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
                .body(new ErrorResponse(
                        "CLOUDINARY_UPLOAD_ERROR",
                        e.getErrorCode()
                ));
    }

    /*
        클라우디너리에 사진을 삭제하지 못한 경우의 핸들러이다.
     */
    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ErrorResponse> handleCloudinaryDeleteException(
            CloudinaryDeleteException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "CLOUDINARY_DELETE_ERROR",
                        e.getErrorCode()
                ));
    }

    /*
        DB에 이미 동일한 닉네임을 가진 유저가 존재하는 경우의 핸들러이다.
     */
    @ExceptionHandler(DuplicateNicknameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(
            DuplicateNicknameException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponse(
                        "DUPLICATE_NICKNAME",
                        e.getErrorCode()
                ));
    }

    /*
        DB에 이미 동일한 이메일을 가진 유저가 존재하는 경우의 핸들러이다.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateNickname(
            DuplicateEmailException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponse(
                        "DUPLICATE_EMAIL",
                        e.getErrorCode()
                ));
    }

    /*
        DB에 태그가 존재하지 않을 경우의 핸들러이다.
     */
    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            TagNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "TAG_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        DB에 댓글이 존재하지 않을 경우의 핸들러이다.
     */
    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            CommentNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "COMMENT_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        올바르지 않은 유저가 댓글 수정/삭제를 시도하는 경우의 핸들러이다.
     */
    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            CommentAccessDeniedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "COMMENT_ACCESS_DENIED",
                        e.getErrorCode()
                ));
    }

    /*
        챌린지 방장이 챌린지에서 나가기를 시도하는 경우의 핸들러이다.
     */
    @ExceptionHandler(ChallengeOwnerCannotLeaveException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            ChallengeOwnerCannotLeaveException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "CHALLENGE_OWNER_CANNOT_LEAVE",
                        e.getErrorCode()
                ));
    }

    /*
        챌린지에 관한 행동 중, 허용되지 않은 행위를 수행할 때 발생하는 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(NotAllowedChallengeActionException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(
            NotAllowedChallengeActionException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        "NOT_ALLOWED_CHALLENGE_ACTION",
                        e.getErrorCode()
                ));
    }

    /*
        이미지에 관한 처리 중, 검증에 실패하거나 최대 이미지 개수를 초과하는 등의 클라이언트의 요청 오류가 발생하는 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(
                InvalidImageException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_IMAGE",
                        e.getErrorCode()
                ));

    }

    /*
        같은 챌린지에 대해 하루에 두 번 중복하여 인증하는 경우를 처리하는 핸들러이다.
    */
    @ExceptionHandler(DuplicateDailyPostException.class)
    public ResponseEntity<ErrorResponse>  handleCloudinaryError(
            DuplicateDailyPostException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponse(
                        "DUPLICATION_UPLOAD_ERROR",
                        e.getErrorCode()
                ));
    }

    /*
        현재 챌린지의 상태가 올바르지 않은 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(ChallengeNotInProgressException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            ChallengeNotInProgressException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "CHALLENGE_STATE_ERROR",
                        e.getErrorCode()
                ));
    }

    /*
        같은 게시글에 좋아요를 두 번 누른 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(DuplicatePostLikeException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            DuplicatePostLikeException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "DUPLICATION_POST_LIKE_ERROR",
                        e.getMessage()
                ));
    }

    /*
        인증글 좋아요를 찾지 못한 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(PostLikeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            PostLikeNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "POST_LIKE_NOT_FOUND",
                        e.getMessage()
                ));
    }

    /*
        Spring AI의 문서 처리 과정에서 생기는 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            DocumentProcessingException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "DOCUMENT_PROCESSING_FAILED",
                        e.getMessage()
                ));
    }

    /*
        Spring AI의 응답 생성 과정에서 생기는 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(AiGenerationException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            AiGenerationException e
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        "AI_RESPONSE_GENERATION_FAILED",
                        e.getMessage()
                ));
    }

    /*
        벡터 스토어에 문서를 저장하는 과정에서 파일의 형식이 맞지 않아 생기는 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidDocumentFormatException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            InvalidDocumentFormatException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_FILE_FORMAT",
                        e.getMessage()
                ));
    }

    /*
        문서가 존재하지 않아 생기는 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            InvalidDocumentException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_DOCUMENT_ID",
                        e.getMessage()
                ));
    }

    /*
        AI에게 보내는 쿼리가 유효하지 않은 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            InvalidQueryException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_QUERY_FORMAT",
                        e.getMessage()
                ));
    }

    /*
        유저 통계를 검색하지 못한 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(UserStatisticsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            UserStatisticsNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "USERSTATISTICS_NOT_FOUND",
                        e.getMessage()
                ));
    }

    /*
        CLIP 서버 응답의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(ClipServerException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            ClipServerException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        "EMPTY_RESPONSE",
                        e.getMessage()
                ));
    }

    /*
        신고를 검색하지 못한 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            ReportNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "REPORT_NOT_FOUND",
                        e.getMessage()
                ));
    }

    /*
        같은 신고에 대해 중복하여 신고하는 경우를 처리하는 핸들러이다.
    */
    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse>  handleCloudinaryError(
            DuplicateReportException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)  // 409
                .body(new ErrorResponse(
                        "DUPLICATION_REPORT_ERROR",
                        e.getErrorCode()
                ));
    }

    /*
        객체와 신고내역의 객체 id가 같지 않은 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidReportTargetException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            InvalidReportTargetException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "NOT_EQUAL_ID",
                        e.getMessage()
                ));
    }

    /*
        챌린지 초대를 검색하지 못한 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(ChallengeInviteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            ChallengeInviteNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "CHALLENGE_INVITE_NOT_FOUND",
                        e.getMessage()
                ));
    }

    /*
        업로드 한 지 하루가 지난 경우에 인증글을 업데이트 하는 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(PostModificationPeriodExpiredException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            PostModificationPeriodExpiredException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "POST_MODIFICATION_PERIOD_EXPIRED",
                        e.getMessage()
                ));
    }

    /*
        올바르지 않은 연, 월, 날짜가 들어온 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidDateException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            InvalidDateException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "DATE_INVALID",
                        e.getMessage()
                ));
    }

    /*
        공유 이미지를 조회하지 못한 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(ShareImageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            ShareImageNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        "NOT_FOUND_IMAGE",
                        e.getMessage()
                ));
    }

    /*
        어드민으로써 수행하면 안되는 행위를 한 경우의 예외를 처리하는 핸들러이다.
     */
    @ExceptionHandler(NotAllowedAdminException.class)
    public ResponseEntity<ErrorResponse> handleChallengeNotInProgress(
            NotAllowedAdminException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        "ADMIN_ERROR",
                        e.getMessage()
                ));
    }

    /*
        잔디 db 생성중, 올바르지 않은 연도가 들어오는 경우를 처리하는 핸들러이다.
     */
    @ExceptionHandler(InvalidYearException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(
            InvalidYearException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        "INVALID_YEAR",
                        e.getErrorCode()
                ));

    }

    /*
        관리자 로그인/회원가입 예외 중 401을 처리하는 핸들러이다.
     */
    @ExceptionHandler(AdminAuthUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthUnauthorized(
            AdminAuthUnauthorizedException e
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401
                .body(new ErrorResponse(
                        "ADMIN_AUTH_UNAUTHORIZED",
                        e.getErrorCode()
                ));
    }

    /*
        관리자 로그인/회원가입 예외 중 403을 처리하는 핸들러이다.
     */
    @ExceptionHandler(AdminAuthForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthForbidden(
            AdminAuthForbiddenException e
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN) // 403
                .body(new ErrorResponse(
                        "ADMIN_AUTH_FORBIDDEN",
                        e.getErrorCode()
                ));
    }

    /*
        관리자 로그인/회원가입 예외 중 404를 처리하는 핸들러이다.
    */
    @ExceptionHandler(AdminAuthNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthNotFound(
            AdminAuthNotFoundException e
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND) // 404
                .body(new ErrorResponse(
                        "ADMIN_AUTH_NOT_FOUND",
                        e.getErrorCode()
                ));
    }

    /*
        관리자 로그인/회원가입 예외 중 409를 처리하는 핸들러이다.
     */
    @ExceptionHandler(AdminAuthConflictException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthConflict(
            AdminAuthConflictException e
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409
                .body(new ErrorResponse(
                        "ADMIN_AUTH_CONFLICT",
                        e.getErrorCode()
                ));
    }

    /*
        관리자 로그인/회원가입 예외 중 400를 처리하는 핸들러이다.
     */
    @ExceptionHandler(AdminAuthBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleAdminAuthBadRequest(
            AdminAuthBadRequestException e
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(new ErrorResponse(
                        "ADMIN_AUTH_BAD_REQUEST",
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