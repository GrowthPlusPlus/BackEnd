package me.jsu.springbootdeveloper.challenge.dto.invite;

import lombok.Getter;
import me.jsu.springbootdeveloper.user.domain.User;

@Getter
public class ChallengeFriendInviteListResponse {
    private final Long userId;
    private final String nickname;
    private final String profileImageUrl;
    private final String inviteStatus; //"INVITED", "AVAILABLE"

    public ChallengeFriendInviteListResponse(User user, String profileImageUrl, boolean isInvited) {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.profileImageUrl = profileImageUrl;
        if (isInvited) {
            this.inviteStatus = "INVITED";
        } else {
            this.inviteStatus = "AVAILABLE";
        }
    }
}
