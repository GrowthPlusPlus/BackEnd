// 정성우

package me.jsu.springbootdeveloper.dto.mainHome;

import lombok.Getter;

import java.util.List;

/*
    알람 정보와 더 읽어올 알람이 있는지 알려주는 불린 값을 포함하여 실제로 클라이언트에게 전달하는 dto이다.
 */
@Getter
public class MyNotificationResponse {
    private final List<MyNotification> myNotification;
    private final boolean hasNext;  // 더 읽어올 페이지가 있는지 알려준다.

    public MyNotificationResponse(List<MyNotification> myNotification, boolean hasNext) {
        this.myNotification = myNotification;
        this.hasNext = hasNext;
    }
}
