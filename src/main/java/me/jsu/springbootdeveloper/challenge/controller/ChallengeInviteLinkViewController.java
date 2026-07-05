// 정하늘

package me.jsu.springbootdeveloper.challenge.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.dto.invite.ChallengeInviteLinkLandingView;
import me.jsu.springbootdeveloper.challenge.service.ChallengeInviteLinkLandingService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/*
    카카오톡 등 메신저에서 열리는 초대 링크 랜딩 웹페이지 컨트롤러이다.
    /v/{inviteCode} 경로로 HTML 페이지를 반환한다.
 */
@Controller
@RequiredArgsConstructor
public class ChallengeInviteLinkViewController {
    private final ChallengeInviteLinkLandingService challengeInviteLinkLandingService;

    /*
        inviteCode에 해당하는 랜딩 HTML 페이지를 반환한다.
        유효한 링크면 앱 딥링크를 시도하고, 실패 시 앱 미설치 안내를 표시한다.

        @param inviteCode URL 경로의 8자리 초대 코드
        @param model Thymeleaf 템플릿에 전달할 모델
        @return templates/invite/landing.html
     */
    @GetMapping("/v/{inviteCode}")
    public String landingPage(
            @PathVariable String inviteCode,
            Model model
    ) {
        ChallengeInviteLinkLandingView landingView = challengeInviteLinkLandingService.buildLandingView(
                inviteCode
        );

        model.addAttribute("landing", landingView);

        return "invite/landing";
    }

    /*
        카카오톡 OG 미리보기용 기본 SVG 이미지를 반환한다.
        static 파일 없이도 미리보기 썸네일을 제공하기 위한 엔드포인트이다.

        @return SVG 이미지 본문
     */
    @GetMapping(value = "/v/og-default.svg", produces = "image/svg+xml")
    public ResponseEntity<String> defaultOgImage() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630" viewBox="0 0 1200 630">
                  <rect width="1200" height="630" fill="#4338ca"/>
                  <rect x="80" y="80" width="1040" height="470" rx="32" fill="#ffffff" opacity="0.12"/>
                  <text x="600" y="280" fill="#ffffff" font-family="Arial, sans-serif" font-size="64" font-weight="700" text-anchor="middle">Challenge Invite</text>
                  <text x="600" y="360" fill="#e0e7ff" font-family="Arial, sans-serif" font-size="32" text-anchor="middle">Join the challenge together</text>
                </svg>
                """;

        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg);
    }
}
