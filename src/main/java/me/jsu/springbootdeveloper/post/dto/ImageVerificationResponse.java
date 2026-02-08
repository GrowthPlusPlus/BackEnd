// 정성우

package me.jsu.springbootdeveloper.post.dto;

import lombok.Builder;
import lombok.Getter;

/*
    이미지 검증 결과를 유저에게 돌려주는 함수이다.
 */
@Getter
@Builder
public class ImageVerificationResponse {
    private String passed;
}
