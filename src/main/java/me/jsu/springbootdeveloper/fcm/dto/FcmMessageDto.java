// 정성우

package me.jsu.springbootdeveloper.fcm.dto;

import lombok.Getter;

/*
    FCM 푸시 알림 메시지를 담는 DTO이다.
    Firebase Cloud Messaging으로 전송할 메시지의 제목과 내용을 포함한다.
 */
@Getter
public class FcmMessageDto {
    private final String title;  // 푸시 알림 제목
    private final String body;  // 푸시 알림 내용

    public FcmMessageDto(String title, String body) {
        this.title = title;
        this.body = body;
    }
}
