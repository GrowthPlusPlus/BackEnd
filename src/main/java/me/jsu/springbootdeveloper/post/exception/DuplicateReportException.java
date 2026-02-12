// 정성우

package me.jsu.springbootdeveloper.post.exception;

/*
    유저가 같은 객체에 대해 신고를 두 번 이상하는 경우의 예외처리를 위한 클래스이다.
 */
public class DuplicateReportException extends RuntimeException {
  private final String errorCode;

  public DuplicateReportException(String errorCode) {
    super(errorCode);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}