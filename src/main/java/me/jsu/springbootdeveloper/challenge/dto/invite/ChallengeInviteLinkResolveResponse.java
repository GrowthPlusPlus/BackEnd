// 정하늘

package me.jsu.springbootdeveloper.challenge.dto.invite;

import lombok.Getter;

/*
    초대 링크 코드를 해석한 뒤 앱의 챌린지 상세 화면으로 이동하기 위한 DTO이다.
    ChallengeInvite를 생성하지 않으며, challengeId만 전달한다.
 */
@Getter
public class ChallengeInviteLinkResolveResponse {
    private final long challengeId;
    private final String title;
    private final boolean alreadyParticipant;

    public ChallengeInviteLinkResolveResponse(long challengeId, String title, boolean alreadyParticipant) {
        this.challengeId = challengeId;
        this.title = title;
        this.alreadyParticipant = alreadyParticipant;
    }
}
