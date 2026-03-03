// 정성우

package me.jsu.springbootdeveloper.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.notification.dto.MyNotificationResponse;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        // 서비스에서 정제한 정보를 response 객체에 담는다.
        Page<MyNotificationResponse> response = notificationService.getNotification(userDetails.getUsername(), page);

        // 여기서 알림 상태(isRead)를 바꾸면 db값은바뀌어도 dto값은 변하지 않는다. 따라서 알림 상태를 수정한다

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }
}
