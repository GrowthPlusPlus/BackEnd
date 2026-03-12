// 정성우

package me.jsu.springbootdeveloper.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.notification.dto.MotivationNotificationScheduleRequest;
import me.jsu.springbootdeveloper.notification.dto.MotivationNotificationScheduleResponse;
import me.jsu.springbootdeveloper.notification.service.MotivationNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/*
    동기부여 메시지 알림 설정을 관리하는 컨트롤러이다.
    사용자가 매일 받을 동기부여 메시지의 시간을 설정하고 조회할 수 있다.
 */
@RestController
@RequiredArgsConstructor
public class MotivationNotificationController {
    private final MotivationNotificationService motivationNotificationService;

    /*
        사용자의 동기부여 알림 시간을 설정하거나 업데이트하는 API이다.
        기존 설정이 있으면 업데이트하고, 없으면 새로 생성한다.
     */
    @Operation(
            summary = "동기부여 알림 시간 설정",
            description = """
        로그인한 사용자의 동기부여 메시지 알림 시간을 설정하거나 업데이트한다.
        매일 지정된 시간에 동기부여 메시지를 받을 수 있다.
        ex) PUT /api/notification/motivation/schedule
        요청 본문: { "notificationTime": "09:00", "enabled": true }
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 시간 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (시간 형식 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/api/notification/motivation/schedule")
    public ResponseEntity<Void> setNotificationSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody MotivationNotificationScheduleRequest request
    ) {
        // 1. 현재 로그인한 사용자의 이메일을 가져온다.
        String email = userDetails.getUsername();

        // 2. 서비스에 위임하여 알림 시간을 설정하거나 업데이트한다.
        motivationNotificationService.setNotificationSchedule(
                email,
                request.getNotificationTime(),
                request.getEnabled()
        );

        // 3. 별도의 응답 바디 없이 200 OK만 반환한다.
        return ResponseEntity.ok().build();
    }

    /*
        사용자의 동기부여 알림 시간 설정을 조회하는 API이다.
        설정이 없으면 null을 반환한다.
     */
    @Operation(
            summary = "동기부여 알림 시간 설정 조회",
            description = """
        로그인한 사용자의 동기부여 메시지 알림 시간 설정을 조회한다.
        설정이 없으면 null을 반환한다.
        ex) GET /api/notification/motivation/schedule
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 시간 설정 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/api/notification/motivation/schedule")
    public ResponseEntity<MotivationNotificationScheduleResponse> getNotificationSchedule(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 1. 현재 로그인한 사용자의 이메일을 가져온다.
        String email = userDetails.getUsername();

        // 2. 서비스에서 알림 시간 설정을 조회한다.
        var schedule = motivationNotificationService.getNotificationSchedule(email);

        // 3. 엔티티를 DTO로 변환하여 반환한다. (설정이 없으면 null 반환)
        MotivationNotificationScheduleResponse response = MotivationNotificationScheduleResponse.from(schedule);
        return ResponseEntity.ok(response);
    }
}
