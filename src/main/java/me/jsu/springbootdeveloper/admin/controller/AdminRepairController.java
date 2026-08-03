// 정성우

package me.jsu.springbootdeveloper.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.admin.service.AdminRepairService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/*
    기존과 정책이 바뀌거나 논리 오류로 정합성 문제가 생긴 경우, 해당 문제를 해결해주는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class AdminRepairController {
    private final AdminRepairService adminRepairService;

    /*
        유저 통계의 총 수행 일자의 정합성을 맞춘다.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(
            summary = "유저 통계 수행일자의 정합성",
            description = """
        모든 유저 통계 수행일자의 정합성을 맞춘다.
        ex) POST /api/admin/repair/user-statistics/total-active-days
        """
    )
    @PostMapping("/api/admin/repair/user-statistics/total-active-days")
    public ResponseEntity<Void> rebuildTotalActiveDays() {
        adminRepairService.rebuildTotalActiveDays();

        return ResponseEntity.noContent().build();
    }
}