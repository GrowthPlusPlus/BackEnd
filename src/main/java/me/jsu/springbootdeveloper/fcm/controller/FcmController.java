// 정성우

package me.jsu.springbootdeveloper.fcm.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.fcm.dto.FcmNotificationSettingDto;
import me.jsu.springbootdeveloper.fcm.dto.FcmTokenRequestDto;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/*
    FCM(Firebase Cloud Messaging) 관련 API를 처리하는 컨트롤러이다.
    FCM 토큰 등록, 업데이트, 삭제 기능을 제공한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fcm")
public class FcmController {
    private final UserService userService;

    /*
        FCM 토큰을 등록하거나 업데이트하는 API이다.
        앱에서 FCM 토큰을 받아 서버에 저장한다.
     */
    @Operation(
            summary = "FCM 토큰 등록/업데이트",
            description = """
        로그인한 사용자의 FCM 토큰을 등록하거나 업데이트한다.
        앱 설치 후 또는 토큰 갱신 시 호출한다.
        ex) PUT /api/fcm/token
        요청 본문: { "fcmToken": "사용자의_FCM_토큰" }
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "FCM 토큰 등록/업데이트 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/token")
    public ResponseEntity<Void> registerOrUpdateFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FcmTokenRequestDto request
    ) {
        // 로그인한 사용자의 FCM 토큰을 등록하거나 업데이트한다.
        userService.updateFcmToken(userDetails.getUsername(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    /*
        FCM 토큰을 삭제하는 API이다.
        로그아웃 시 호출하여 더 이상 푸시 알림을 받지 않도록 한다.
     */
    @Operation(
            summary = "FCM 토큰 삭제",
            description = """
        로그인한 사용자의 FCM 토큰을 삭제한다.
        로그아웃 시 호출하여 더 이상 푸시 알림을 받지 않도록 한다.
        ex) DELETE /api/fcm/token
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "FCM 토큰 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/token")
    public ResponseEntity<Void> deleteFcmToken(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        // 로그인한 사용자의 FCM 토큰을 삭제한다.
        userService.clearFcmToken(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /*
        친구 관련 푸시 알림 활성화/비활성화를 설정하는 API이다.
     */
    @Operation(
            summary = "친구 관련 푸시 알림 설정",
            description = """
        로그인한 사용자의 친구 관련 푸시 알림을 활성화하거나 비활성화한다.
        활성화 시: 친구 신청, 친구 신청 수락 등의 푸시 알림을 받는다.
        비활성화 시: 친구 관련 푸시 알림을 받지 않는다.
        ex) PUT /api/fcm/notification/friend
        요청 본문: { "enabled": true } 또는 { "enabled": false }
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "푸시 알림 설정 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/notification/friend")
    public ResponseEntity<Void> setFriendPushNotificationEnabled(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FcmNotificationSettingDto request
    ) {
        // 로그인한 사용자의 친구 관련 푸시 알림 활성화 여부를 설정한다.
        userService.setFriendPushNotificationEnabled(userDetails.getUsername(), request.isEnabled());
        return ResponseEntity.ok().build();
    }
}
