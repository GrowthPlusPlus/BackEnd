// 정성우

package me.jsu.springbootdeveloper.dto.error;

public record ErrorResponse(
        String error,
        String reason
) {}