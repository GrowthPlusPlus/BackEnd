package me.jsu.springbootdeveloper.user.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.user.domain.User;

@Getter
public class FriendListResponse {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    public FriendListResponse(User user, String profileImageUrl) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.nickname = user.getNickname();
        this.profileImageUrl = profileImageUrl;
    }
}
