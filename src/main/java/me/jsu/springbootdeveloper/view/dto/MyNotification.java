// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.notification.domain.Notification;

/*
    알림에서 필요한 정보만 정제하여 제공한다. 매핑을 위한 클래스이다.
 */
@Getter
public class MyNotification {
    //private final String title;  알림 제목이 챌린지 생성 알림에 쓰이지 않아 일시적으로 주석처리
    private final String message;  // 알림 내용이다.
    private final boolean isRead;  // 읽음 여부를 나타낸다.

    public MyNotification(Notification notification) {
        //this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.isRead = notification.isRead();
    }
}
