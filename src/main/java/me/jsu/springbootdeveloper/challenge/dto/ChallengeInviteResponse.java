package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Getter;

import java.util.List;

/*
    챌린지 초대 정보를 담는 DTO이다.
    초대 링크와 친구 목록(초대 상태 포함)을 함께 반환한다.
 */
@Getter
public class ChallengeInviteResponse {
    private final String inviteLink;  // 챌린지 초대 링크
    private final List<ChallengeFriendInviteListResponse> reponseList;  // 친구 목록과 각 친구의 초대 상태

    public ChallengeInviteResponse(String inviteLink, List<ChallengeFriendInviteListResponse> reponseList) {
        this.inviteLink = inviteLink;
        this.reponseList = reponseList;
    }
}
