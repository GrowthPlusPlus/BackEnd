// 정성우

package me.jsu.springbootdeveloper.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.notification.dto.MyNotificationResponse;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @Operation(
            summary = "알림 목록 조회",
            description = """
        로그인한 사용자의 알림 목록을 페이지 단위로 조회하여 반환한다.
        프론트엔드에서는 스크롤 또는 페이지 이동 시 page 값을 증가시켜 호출한다.
        ex) GET /api/notification?page=0
        - 26.3.4 알림 읽음 표시 추가
        - 처음 조회한 알림의 isRead 값: false
        - 두번이상 조회한 알림의 isRead 값: true
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 알림 데이터를 찾을 수 없음")
    })
    @GetMapping("/api/notification")
    public ResponseEntity<Page<MyNotificationResponse>> getNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "페이지 번호 (0부터 시작)", required = true, example = "0")
            @RequestParam int page)
    {
        // 서비스에서 알림 목록을 조회하고 읽음 처리까지 수행한다.
        // DTO에는 읽음 처리 전 원래 isRead 상태가 담긴다.
        Page<MyNotificationResponse> response = notificationService.getNotifications(userDetails.getUsername(), page);

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }

    /*
        프론트에서 사용자가 현재 페이지에서 선택한 여러 알림을 한 번에
        읽음 처리할 수 있도록 하는 API이다.

        요청 바디로 알림 ID 리스트를 전달하면, 서버는 로그인한 사용자의
        알림들 중 해당 ID들을 한 번에 읽음 처리한다.
        (정책 B: 존재하지 않거나 다른 사용자의 알림 ID는 조용히 무시한다.)

        @param userDetails  현재 로그인한 사용자 정보이다.
        @param notificationIds  읽음 처리할 알림 ID 리스트이다.
        @return 내용(body) 없이 200 OK 상태만 반환한다.
     */
    @Operation(
            summary = "알림 읽음 처리",
            description = """
        프론트에서 전달한 알림 ID 리스트를 기준으로,
        로그인한 사용자의 알림들만 한 번에 읽음 처리한다.
        존재하지 않는 ID나 다른 사용자의 알림은 무시되며,
        성공/실패 여부는 HTTP 상태 코드(200/401 등)로만 표현한다.
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/api/notification/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody List<Long> notificationIds)
    {
        // 1. 현재 로그인한 사용자의 이메일을 가져온다.
        String email = userDetails.getUsername();

        // 2. 서비스에 위임하여 여러 알림을 한 번에 읽음 처리한다.
        notificationService.markNotificationsAsRead(email, notificationIds);

        // 3. 별도의 응답 바디 없이 200 OK만 반환한다.
        return ResponseEntity.ok().build();
    }
}
