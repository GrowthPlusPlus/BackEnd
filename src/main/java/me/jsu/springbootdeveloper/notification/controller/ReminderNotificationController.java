// 정성우

package me.jsu.springbootdeveloper.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;
import me.jsu.springbootdeveloper.notification.dto.ReminderNotificationTimeDto;
import me.jsu.springbootdeveloper.notification.repository.UserNotificationScheduleRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

/*
    실패 방지 리마인더(주간 리마인더)의 전역 시간 설정/조회를 담당하는 컨트롤러이다.
    - 사용자는 일요일에 실패 방지 리마인더를 받을 "시(hour)"를 1시간 단위로 설정할 수 있다.
    - 실제 발송은 ChallengeReminderScheduler/ChallengeReminderNotificationService에서 처리한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReminderNotificationController {

    private final UserService userService;
    private final UserNotificationScheduleRepository userNotificationScheduleRepository;

    /*
        실패 방지 리마인더 전역 시간을 설정하거나 업데이트하는 API이다.
        - 분/초 단위는 모두 0으로 고정되며, 시(hour)만 의미가 있다.
     */
    @Operation(
            summary = "실패 방지 리마인더 시간 설정",
            description = """
        로그인한 사용자의 실패 방지 리마인더(주간 리마인더) 시간을 설정하거나 업데이트한다.
        시간은 1시간 단위(HH:00)로만 설정할 수 있다.

        ex) PUT /api/notification/reminder/time
        요청 본문: { "notificationTime": "21:00" }
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 시간 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (시간 형식 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @Transactional
    @PutMapping("/api/notification/reminder/time")
    public ResponseEntity<Void> setReminderNotificationTime(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReminderNotificationTimeDto request
    ) {
        String email = userDetails.getUsername();
        User user = userService.findByEmail(email);

        // 1. 분/초/나노초를 0으로 정규화한다.
        LocalTime rawTime = request.getNotificationTime();
        LocalTime normalizedTime = rawTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        // 2. 시(hour) 단위가 아닌 값(분이 0이 아닌 값)이 들어오면 에러를 발생시킨다.
        if (rawTime.getMinute() != 0) {
            throw new IllegalArgumentException("리마인더 시간은 시(hour) 단위로만 설정할 수 있습니다. 예: 21:00");
        }

        userNotificationScheduleRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(
                        schedule -> {
                            schedule.updateNotificationTime(normalizedTime);
                            log.info("실패 방지 리마인더 시간을 업데이트했습니다. 사용자: {}, 시간: {}",
                                    email, normalizedTime);
                        },
                        () -> {
                            UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                    .user(user)
                                    .notificationTime(normalizedTime)
                                    .enabled(false)  // 동기부여 알림은 기본적으로 비활성화
                                    .dailyReminderPushNotificationEnabled(true)  // 리마인더 전역 토글은 기본 ON
                                    .build();
                            userNotificationScheduleRepository.save(newSchedule);
                            log.info("실패 방지 리마인더 시간을 생성했습니다. 사용자: {}, 시간: {}",
                                    email, normalizedTime);
                        }
                );

        return ResponseEntity.ok().build();
    }

    /*
        실패 방지 리마인더 전역 시간을 조회하는 API이다.
        설정이 없으면 200 OK와 함께 null을 반환한다.
     */
    @Operation(
            summary = "실패 방지 리마인더 시간 조회",
            description = """
        로그인한 사용자의 실패 방지 리마인더(주간 리마인더) 시간을 조회한다.
        설정이 없으면 null을 반환한다.

        ex) GET /api/notification/reminder/time
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리마인더 시간 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/api/notification/reminder/time")
    public ResponseEntity<ReminderNotificationTimeDto> getReminderNotificationTime(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userService.findByEmail(email);

        UserNotificationSchedule schedule = userNotificationScheduleRepository
                .findByUser_Id(user.getId())
                .orElse(null);

        if (schedule == null) {
            return ResponseEntity.ok(null);
        }

        // notificationTime은 항상 HH:00 형태로 저장되어 있다고 가정한다.
        ReminderNotificationTimeDto response = new ReminderNotificationTimeDto(
                schedule.getNotificationTime()
        );
        return ResponseEntity.ok(response);
    }
}

