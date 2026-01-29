package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Getter;

/*
    클라이언트에게 챌린지의 맴버 목록을 보낸다.
    챌린지의 맴버를 관리하기 위해서나 방장을 위임하기 위해 보내는 정보이다.
 */
@Getter
public class ChallengeMemberResponse {
    private final Long userId;
    private final String nickname;
    private final String profileImageUrl;

    public ChallengeMemberResponse(Long userId, String nickname, String profileImageUrl) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}