package me.jsu.springbootdeveloper.dto.createChallenge;

import lombok.Getter;
import me.jsu.springbootdeveloper.dto.FriendListResponse;

import java.util.List;

@Getter
public class CreateChallengeResponse {
    private Long id;
    //챌린지 생성 링크
    private String challengeLink;
    private List<FriendListResponse> friends;
    public CreateChallengeResponse(String challengeLink, List<FriendListResponse> friends) {
        this.challengeLink = challengeLink;
        this.friends = friends;
    }
}
