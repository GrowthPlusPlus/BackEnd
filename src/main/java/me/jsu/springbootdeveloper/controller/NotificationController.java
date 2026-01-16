// 정성우

package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.mainHome.MainHomeResponse;
import me.jsu.springbootdeveloper.dto.mainHome.MyNotificationResponse;
import me.jsu.springbootdeveloper.service.NotificationService;
import me.jsu.springbootdeveloper.service.view.MainHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;

/*
    알람에 관련한 모든 동작들을 수행하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    /*
        프론트에서 GET /api/notifications?page=번호 형태로 스크롤 등의 행위로 추가적인 정보가 필요할때 page 번호를 업데이트 시켜 호출한다.
     */
    @GetMapping("/api/notification")
    public ResponseEntity<MyNotificationResponse> getMainHome(
            @AuthenticationPrincipal UserDetails userDetails, @RequestParam int page) {

        // 서비스에서 정제한 정보를 response 객체에 담는다.
        MyNotificationResponse response = notificationService.getNotification(userDetails.getUsername(), page);

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }
}
