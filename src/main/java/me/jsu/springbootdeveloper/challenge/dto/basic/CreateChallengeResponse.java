package me.jsu.springbootdeveloper.challenge.dto.basic;

import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.user.dto.FriendListResponse;

import java.util.List;

@Getter
public class CreateChallengeResponse {
    private Long id;
    private String title;
    //챌린지 생성 링크
    private String challengeLink;
    private List<FriendListResponse> friends;
    public CreateChallengeResponse(Challenge challenge, String challengeLink, List<FriendListResponse> friends) {
        this.id = challenge.getId();
        this.title = challenge.getTitle();
        this.challengeLink = challengeLink;
        this.friends = friends;

    }
}
