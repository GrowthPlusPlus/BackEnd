package me.jsu.springbootdeveloper.post.exception;

public class NotFoundException extends RuntimeException {
    private final String errorCode;

    public NotFoundException(String errorCode) {
        super(errorCode);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}