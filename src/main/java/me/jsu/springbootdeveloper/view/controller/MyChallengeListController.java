// 정성우

package me.jsu.springbootdeveloper.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.view.dto.MyChallengeInProgressResponse;
import me.jsu.springbootdeveloper.view.dto.MyChallengeSuccessOrFailResponse;
import me.jsu.springbootdeveloper.view.service.MyChallengeListService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    유저가 수행하는 진행중, 완료, 실패 챌린지 리스트를 볼 수 있는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class MyChallengeListController {
    private final MyChallengeListService myChallengeListService;

    /*
        유저가 참여한 챌린지 중, 진행중인 챌린지 리스트를 조회한다.
     */
    @Operation(
            summary = "내 진행 중인 챌린지 목록 조회",
            description = """
        로그인한 사용자가 현재 참여 중인 챌린지 목록을 조회한다.
        onlyTwo를 true로 하면 마이페이지의 화면에 보여줄 챌린지 2개만 가져올 수 있다.
        onlyTwo를 false로 하면 챌린지 리스트 상세보기에 보여줄 챌린지들을 가져온다.
        ex) GET /api/challenges/my/inProgress?onlyTwo=true
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진행 중인 챌린지 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/challenges/my/inProgress")
    public ResponseEntity<List<MyChallengeInProgressResponse>> getMyChallengesInProgress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 목록 개수", example = "true")
            @RequestParam(defaultValue = "false") boolean onlyTwo
    ) {
        List<MyChallengeInProgressResponse> responses = myChallengeListService.getMyChallenges(userDetails.getUsername(), Status.IN_PROGRESS, onlyTwo);

        return ResponseEntity.ok(responses);
    }

    /*
        유저가 참여한 챌린지 중, 완료한 챌린지 리스트를 조회한다.
     */
    @Operation(
            summary = "내 완료 챌린지 목록 조회",
            description = """
        로그인한 사용자가 완료한 챌린지 목록을 조회한다.
        완료일은 챌린지 참여 기록의 completedAt 기준으로 반환된다.
        onlyTwo를 true로 하면 마이페이지의 화면에 보여줄 챌린지 2개만 가져올 수 있다.
        onlyTwo를 false로 하면 챌린지 리스트 상세보기에 보여줄 챌린지들을 가져온다.
        ex) GET /api/challenges/my/success?onlyTwo=true
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "완료한 챌린지 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/challenges/my/success")
    public ResponseEntity<List<MyChallengeSuccessOrFailResponse>> getMyChallengesSuccess(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 목록 개수", example = "true")
            @RequestParam(defaultValue = "false") boolean onlyTwo
    ) {
        List<MyChallengeSuccessOrFailResponse> responses = myChallengeListService.getMyChallengesSuccessOrFail(userDetails.getUsername(), Status.SUCCESS, onlyTwo);

        return ResponseEntity.ok(responses);
    }

    /*
        유저가 참여한 챌린지 중, 실패한 챌린지 리스트를 조회한다.
     */
    @Operation(
            summary = "내 실패 챌린지 목록 조회",
            description = """
        로그인한 사용자가 실패한 챌린지 목록을 조회한다.
        실패일은 챌린지 참여 기록의 failedAt 기준으로 반환된다.
        onlyTwo를 true로 하면 마이페이지의 화면에 보여줄 챌린지 2개만 가져올 수 있다.
        onlyTwo를 false로 하면 챌린지 리스트 상세보기에 보여줄 챌린지들을 가져온다.
        ex) GET /api/challenges/my/fail?onlyTwo=true
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "실패한 챌린지 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/api/challenges/my/fail")
    public ResponseEntity<List<MyChallengeSuccessOrFailResponse>> getMyChallengesFail(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 목록 개수", example = "true")
            @RequestParam(defaultValue = "false") boolean onlyTwo
    ) {
        List<MyChallengeSuccessOrFailResponse> responses = myChallengeListService.getMyChallengesSuccessOrFail(userDetails.getUsername(), Status.FAIL, onlyTwo);

        return ResponseEntity.ok(responses);
    }
}
