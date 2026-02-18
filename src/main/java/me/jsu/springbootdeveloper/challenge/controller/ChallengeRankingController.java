package me.jsu.springbootdeveloper.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
// 정성우

import me.jsu.springbootdeveloper.challenge.dto.RankingResultResponse;
import me.jsu.springbootdeveloper.challenge.service.ChallengeRankingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

/*
    해당 챌린지의 사용자 랭킹을 조회하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeRankingController {
    private final ChallengeRankingService challengeRankingService;

    /*
        특정 챌린지의 랭킹을 조회한다.
        상위 100명 랭킹과 로그인한 사용자의 랭킹을 조회한다.
     */
    @Operation(
            summary = "챌린지 랭킹 조회",
            description = """
        상위 100명 랭킹과 로그인 사용자의 랭킹을 반환한다.
        챌린지 스코어는 매일 11시 50분에 집계된다.
        ex) GET /api/challenges/1/ranking
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "랭킹 조회 성공"),
            @ApiResponse(responseCode = "404", description = "챌린지 또는 참여 정보 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/ranking")
    public ResponseEntity<RankingResultResponse> getRanking(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "챌린지 id", required = true)
            @PathVariable Long challengeId
    ) {
        RankingResultResponse response = challengeRankingService.getRanking(userDetails.getUsername(), challengeId);

        return ResponseEntity.ok(response);
    }
}
