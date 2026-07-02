// 정성우

package me.jsu.springbootdeveloper.post.dto.verify;

import lombok.Builder;
import lombok.Getter;

/*
    임시로 업로드한 파일의 id를 가진 dto이다.
 */
@Getter
@Builder
public class ImageVerificationResponse {
    private Long tempImageId;
}
