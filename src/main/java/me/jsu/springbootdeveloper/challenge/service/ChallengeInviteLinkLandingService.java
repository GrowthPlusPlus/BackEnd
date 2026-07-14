// 정하늘

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import me.jsu.springbootdeveloper.challenge.dto.invite.ChallengeInviteLinkLandingView;
import me.jsu.springbootdeveloper.challenge.enumType.InviteLinkLandingStatus;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteLinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/*
    초대 링크 랜딩 웹페이지에 필요한 데이터를 조회하는 서비스이다.
    인증 없이 inviteCode만으로 페이지를 구성한다.
 */
@RequiredArgsConstructor
@Service
public class ChallengeInviteLinkLandingService {
    private final ChallengeInviteLinkRepository challengeInviteLinkRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    /*
        앱 딥링크 URL 스킴이다.
        앱 팀과 맞춘 값으로 APP_DEEP_LINK_SCHEME 환경 변수로 덮어쓸 수 있다.
     */
    @Value("${app.deep-link-scheme:haenaem}")
    private String deepLinkScheme;

    /*
        inviteCode로 랜딩 페이지에 필요한 View 데이터를 만든다.

        @param inviteCode URL 경로의 8자리 초대 코드
        @return Thymeleaf 템플릿에 전달할 ChallengeInviteLinkLandingView
     */
    @Transactional(readOnly = true)
    public ChallengeInviteLinkLandingView buildLandingView(String inviteCode) {
        String normalizedInviteCode = normalizeInviteCode(inviteCode);
        String pageUrl = baseUrl + "/v/" + normalizedInviteCode;
        String deepLinkUrl = deepLinkScheme + "://invite/" + normalizedInviteCode;
        // 카카오톡 OG 미리보기용 기본 이미지 URL이다.
        String defaultOgImageUrl = baseUrl + "/v/og-default.svg";

        return challengeInviteLinkRepository.findByInviteCode(normalizedInviteCode)
                .map(link -> buildViewFromLink(
                        normalizedInviteCode,
                        link,
                        pageUrl,
                        deepLinkUrl,
                        defaultOgImageUrl
                ))
                .orElseGet(() -> buildNotFoundView(
                        normalizedInviteCode,
                        pageUrl,
                        deepLinkUrl,
                        defaultOgImageUrl
                ));
    }

    private ChallengeInviteLinkLandingView buildViewFromLink(
            String inviteCode,
            ChallengeInviteLink link,
            String pageUrl,
            String deepLinkUrl,
            String defaultOgImageUrl
    ) {
        Challenge challenge = link.getChallenge();
        String challengeTitle = challenge.getTitle();
        String inviterNickname = link.getUser().getNickname();
        LocalDate today = LocalDate.now();

        if (challenge.getEndDate().isBefore(today)) {
            return new ChallengeInviteLinkLandingView(
                    inviteCode,
                    InviteLinkLandingStatus.CHALLENGE_ENDED,
                    challengeTitle,
                    inviterNickname,
                    pageUrl,
                    deepLinkUrl,
                    challengeTitle + " 챌린지 초대",
                    "종료된 챌린지입니다.",
                    defaultOgImageUrl,
                    "종료된 챌린지입니다."
            );
        }

        if (!link.canBeUsed()) {
            return new ChallengeInviteLinkLandingView(
                    inviteCode,
                    InviteLinkLandingStatus.EXPIRED,
                    challengeTitle,
                    inviterNickname,
                    pageUrl,
                    deepLinkUrl,
                    challengeTitle + " 챌린지 초대",
                    "만료된 초대 링크입니다.",
                    defaultOgImageUrl,
                    "만료된 초대 링크입니다."
            );
        }

        String ogDescription = inviterNickname + "님이 '" + challengeTitle + "' 챌린지에 초대했어요.";

        return new ChallengeInviteLinkLandingView(
                inviteCode,
                InviteLinkLandingStatus.VALID,
                challengeTitle,
                inviterNickname,
                pageUrl,
                deepLinkUrl,
                challengeTitle + " 챌린지 초대",
                ogDescription,
                defaultOgImageUrl,
                null
        );
    }

    private ChallengeInviteLinkLandingView buildNotFoundView(
            String inviteCode,
            String pageUrl,
            String deepLinkUrl,
            String defaultOgImageUrl
    ) {
        return new ChallengeInviteLinkLandingView(
                inviteCode,
                InviteLinkLandingStatus.NOT_FOUND,
                "챌린지 초대",
                null,
                pageUrl,
                deepLinkUrl,
                "챌린지 초대",
                "유효하지 않은 초대 링크입니다.",
                defaultOgImageUrl,
                "유효하지 않은 초대 링크입니다."
        );
    }

    private String normalizeInviteCode(String inviteCode) {
        if (inviteCode == null) {
            return "";
        }

        return inviteCode.trim();
    }
}
