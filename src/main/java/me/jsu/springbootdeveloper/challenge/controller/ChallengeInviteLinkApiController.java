// 정하늘

package me.jsu.springbootdeveloper.challenge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.dto.invite.ChallengeInviteLinkResolveResponse;
import me.jsu.springbootdeveloper.challenge.service.ChallengeInviteLinkResolveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/*
    초대 링크 코드를 앱에서 해석하기 위한 API 컨트롤러이다.
    resolve 결과의 challengeId로 앱의 챌린지 상세 화면으로 이동한다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeInviteLinkApiController {
    private final ChallengeInviteLinkResolveService challengeInviteLinkResolveService;

    /*
        inviteCode를 해석해 챌린지 상세 화면 이동에 필요한 challengeId를 반환한다.

        @param userDetails JWT로 인증된 유저
        @param inviteCode 초대 링크의 8자리 코드
        @return challengeId와 이미 참여 중인지 여부
     */
    @Operation(
            summary = "초대 링크 코드 해석",
            description = """
        앱이 딥링크로 받은 inviteCode를 서버에 전달한다.
        유효한 링크이면 challengeId를 반환하고, 앱은 챌린지 상세 화면으로 이동한다.
        참여는 상세 화면에서 POST /api/challenges/{challengeId}/participate 로 처리한다.
        ex) GET /api/challenges/invites/links/a1b2c3d4
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 링크 해석 성공"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 실패"),
            @ApiResponse(responseCode = "403", description = "참여 정원 초과"),
            @ApiResponse(responseCode = "404", description = "초대 링크를 찾을 수 없음"),
            @ApiResponse(responseCode = "410", description = "만료된 초대 링크")
    })
    @GetMapping("/api/challenges/invites/links/{inviteCode}")
    public ResponseEntity<ChallengeInviteLinkResolveResponse> resolveInviteLink(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "초대 링크 코드", required = true, example = "a1b2c3d4")
            @PathVariable String inviteCode
    ) {
        ChallengeInviteLinkResolveResponse response = challengeInviteLinkResolveService.resolveInviteLink(
                userDetails.getUsername(),
                inviteCode
        );

        return ResponseEntity.ok(response);
    }
}
