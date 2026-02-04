// 정성우

package me.jsu.springbootdeveloper.user.dto;

import lombok.Builder;
import lombok.Getter;

/*
    마이페이지의 유저 정보를 반환하기 위한 클래스이다.
 */
@Getter
@Builder
public class MyProfileResponse {
    private String nickname;
    private String introduction;
    private String profileImageUrl;
}
