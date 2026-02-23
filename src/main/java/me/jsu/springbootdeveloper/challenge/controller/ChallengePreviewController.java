// 정성우

package me.jsu.springbootdeveloper.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.dto.preview.ChallengePreviewRequest;
import me.jsu.springbootdeveloper.challenge.dto.preview.ChallengePreviewResponse;
import me.jsu.springbootdeveloper.challenge.service.ChallengePreviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
    챌린지를 생성하기 전에, 챌린지 이름을 통해서 해당 챌린지가 사진 검증이 효과적인지 알려주는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengePreviewController {
    private final ChallengePreviewService challengePreviewService;

    /*
        서버에 사진을 올리기 전에, 해당 사진이 올바른 사진인지 검사하는 컨트롤러이다.
        사진 1장을 사용자가 올릴 때마다 수행한다.
     */
    @Operation(
            summary = "챌린지 이름 검사",
            description = """
        해당 챌린지 이름이 사진 검증을 수행했을 때, AI가 효과적으로 판별할 수 있는 이름, 혹은 주제인지 판별한다.
        ex) POST /api/challenge/preview
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이미지 요청 검증 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 검증 실패")
    })
    @PostMapping("/api/challenge/preview")
    public ResponseEntity<ChallengePreviewResponse> verify(
            @Parameter(description = "챌린지 이름", required = true)
            @RequestBody ChallengePreviewRequest request
    ) {
        ChallengePreviewResponse response = challengePreviewService.preview(request.getTitle());

        return ResponseEntity.ok(response);  // 챌린지 이름이 AI가 자동으로 검증하기 적합한지에 대한 여부를 반환한다.
    }
}
