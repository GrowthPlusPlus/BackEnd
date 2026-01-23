package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.repository.user.UserFriendshipSearchProjection;

@Getter
public class UserSearchResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private String relationshipStatus; // FRIEND, PENDING_SENT, PENDING_RECEIVED, NONE

    public UserSearchResponse(UserFriendshipSearchProjection projection) {
        this.userId = projection.getId();
        this.nickname = projection.getNickname();
        this.profileImageUrl = projection.getProfileImage();
        this.relationshipStatus = projection.getStatus();
    }
}
