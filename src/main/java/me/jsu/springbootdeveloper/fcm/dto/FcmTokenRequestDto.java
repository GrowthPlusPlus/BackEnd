// 정성우

package me.jsu.springbootdeveloper.fcm.dto;

import lombok.Getter;

/*
    FCM 토큰 등록/업데이트 요청을 담는 DTO이다.
    앱에서 FCM 토큰을 서버에 등록하거나 업데이트할 때 사용한다.
 */
@Getter
public class FcmTokenRequestDto {
    private String fcmToken;  // Firebase Cloud Messaging 토큰

    public FcmTokenRequestDto() {
    }

    public FcmTokenRequestDto(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
