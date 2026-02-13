// 정성우

package me.jsu.springbootdeveloper.fcm.dto;

import lombok.Getter;

/*
    푸시 알림 설정을 담는 DTO이다.
    친구 관련 푸시 알림 활성화/비활성화 요청에 사용한다.
 */
@Getter
public class FcmNotificationSettingDto {
    private boolean enabled;  // 푸시 알림 활성화 여부

    public FcmNotificationSettingDto() {
    }

    public FcmNotificationSettingDto(boolean enabled) {
        this.enabled = enabled;
    }
}
