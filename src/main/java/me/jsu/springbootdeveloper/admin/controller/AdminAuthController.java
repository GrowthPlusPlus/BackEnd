// 정성우

package me.jsu.springbootdeveloper.admin.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.admin.dto.*;
import me.jsu.springbootdeveloper.admin.service.AdminAuthService;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
    관리자 로그인/회원가입과 관련된 일들을 수행하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class AdminAuthController {
    private final AdminAuthService adminAuthService;
    private final UserService userService;

    /*
        관리자 회원가입 시 사용하는 컨트롤러이다.
     */
    @PostMapping("/api/admin/auth/signup")
    public ResponseEntity<Void> adminSignup(@RequestBody AdminSignupRequest request) {
        adminAuthService.adminSignup(request.getEmail(), request.getPassword(), request.getPasswordVerify(), request.getNickName());

        return ResponseEntity.ok().build();
    }

    /*
        관리자 이메일 인증 시 사용하는 컨트롤러이다.
     */
    @PostMapping("/api/admin/auth/verifyEmail")
    public ResponseEntity<Void> verifyEmail(@RequestBody AdminVerifyEmailRequest request) {
        adminAuthService.verifyAdminEmail(request.getEmail(), request.getCode());

        return ResponseEntity.ok().build();
    }

    /*
        관리자 이메일 인증 코드를 다시 보내는 컨트롤러이다.
     */
    @PostMapping("/api/admin/auth/resendCode")
    public ResponseEntity<Void> resendCode(@RequestParam String email) {
        adminAuthService.resendVerificationCode(email);

        return ResponseEntity.ok().build();
    }

    /*
        관리자로 로그인하는 컨트롤러이다.
     */
    @PostMapping("/api/admin/auth/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminAuthService.adminLogin(request.getEmail(), request.getPassword(), request.getFcmToken());

        return ResponseEntity.ok(response);
    }

    /*
        관리자 회원가입을 승인해주는 api이다. 해당 과정을 거처야지 관리자 계정을 사용할 수 있게 된다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/admin/{userId}/approve")
    public ResponseEntity<Void> approveAdmin(@PathVariable Long userId) {
        adminAuthService.approveAdmin(userId);

        return ResponseEntity.ok().build();
    }

    /*
        관리자 승인 대기중인 계정 목록을 조회하는 API이다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/api/admin/approve/pending")
    public ResponseEntity<List<AdminPendingResponse>> getPendingAdmins() {
        List<AdminPendingResponse> responses = userService.getPendingAdmins();

        return ResponseEntity.ok(responses);
    }
}
