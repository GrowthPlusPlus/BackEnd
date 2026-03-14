// 정성우

package me.jsu.springbootdeveloper.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.dto.share.CalenderShareResponse;
import me.jsu.springbootdeveloper.challenge.service.ChallengeShareImageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/*
    챌린지 캘린더를 공유하기 위한 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeShareImageController {
    private final ChallengeShareImageService shareImageService;

    @Operation(
            summary = "챌린지 캘린더 공유",
            description = """
        사용자의 챌린지 캘린더를 이미지로 생성하여 클라우디너리에 업로드 후, 링크를 반환한다.
        ex) POST /api/share/challenges/1/calendar?year=2026&month=11
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이미지 생성 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "400", description = "연, 월이 잘못 입력됨"),
            @ApiResponse(responseCode = "404", description = "이미지를 찾을 수 없음")
    })
    @PostMapping("/api/share/challenges/{challengeId}/calendar")
    public ResponseEntity<CalenderShareResponse> create(
            @Parameter(description = "챌린지 ID", required = true, example = "1")
            @PathVariable Long challengeId,
            @Parameter(description = "연도", required = true, example = "2026")
            @RequestParam int year,
            @Parameter(description = "월", required = true, example = "11")
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        CalenderShareResponse response = shareImageService.createOrGet(userDetails.getUsername(), challengeId, year, month);

        return ResponseEntity.ok(response);
    }
}
