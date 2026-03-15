// 정성우

package me.jsu.springbootdeveloper.challenge.dto.share;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.enumType.ShareImageStatus;

import java.time.LocalDateTime;

/*
    공유한 캘린더 사진을 반환하는 dto이다.
 */
@Getter
@Builder
public class CalenderShareResponse {
    private final Long challengeId;
    private final String imageUrl;
    private final LocalDateTime updateAt;
    private final ShareImageStatus status;
}
