// 정하늘

package me.jsu.springbootdeveloper.challenge.dto.invite;

import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.enumType.InviteLinkLandingStatus;

/*
    초대 링크 랜딩 웹페이지(Thymeleaf)에 전달할 화면 데이터이다.
 */
@Getter
public class ChallengeInviteLinkLandingView {
    private final String inviteCode;
    private final InviteLinkLandingStatus status;
    private final String challengeTitle;
    private final String inviterNickname;
    private final String pageUrl;
    private final String deepLinkUrl;
    private final String ogTitle;
    private final String ogDescription;
    private final String ogImageUrl;
    private final String statusMessage;

    public ChallengeInviteLinkLandingView(
            String inviteCode,
            InviteLinkLandingStatus status,
            String challengeTitle,
            String inviterNickname,
            String pageUrl,
            String deepLinkUrl,
            String ogTitle,
            String ogDescription,
            String ogImageUrl,
            String statusMessage
    ) {
        this.inviteCode = inviteCode;
        this.status = status;
        this.challengeTitle = challengeTitle;
        this.inviterNickname = inviterNickname;
        this.pageUrl = pageUrl;
        this.deepLinkUrl = deepLinkUrl;
        this.ogTitle = ogTitle;
        this.ogDescription = ogDescription;
        this.ogImageUrl = ogImageUrl;
        this.statusMessage = statusMessage;
    }

    /*
        링크가 유효하여 앱 딥링크를 시도할 수 있는지 여부를 반환한다.
     */
    public boolean isOpenAppEnabled() {
        return status == InviteLinkLandingStatus.VALID;
    }
}
