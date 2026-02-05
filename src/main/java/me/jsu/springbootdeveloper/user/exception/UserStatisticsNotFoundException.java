package me.jsu.springbootdeveloper.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/*
    해당 클래스는 유저 통계를 찾지 못했을 때 발생하는 커스텀 예외 클래스이다.
    HTTP 404 Not Found 상태 코드를 반환하도록 설정한다.
    최초 작성자: 정성우
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserStatisticsNotFoundException extends IllegalArgumentException {
    private final String errorCode;

    public UserStatisticsNotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
