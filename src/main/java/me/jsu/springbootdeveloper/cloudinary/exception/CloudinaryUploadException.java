// 정성우

package me.jsu.springbootdeveloper.cloudinary.exception;

/*
    클라우디너리에 사진을 업로드하지 못한 경우의 예외처리이다.
 */
public class CloudinaryUploadException extends RuntimeException {
    private final String errorCode;

    public CloudinaryUploadException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
