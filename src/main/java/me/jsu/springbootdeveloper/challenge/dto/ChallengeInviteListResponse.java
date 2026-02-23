// 정성우

package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Getter;

import java.util.List;

/*
    챌린지 초대 목록을 반환하기 위한 DTO이다.
    알림 페이지의 챌린지 초대 탭에서 사용된다.
 */
@Getter
public class ChallengeInviteListResponse {
    private final String inviterNickname;  // 초대자 닉네임
    private final long challengeId;
    private final String challengeTitle;  // 챌린지 이름
    private final int participantCount;  // 현재 챌린지에 참여중인 ChallengeParticipant 명수
    private final long remainingDays;  // 챌린지 완료까지 남은 날짜
    private final List<String> tags;  // 해당 챌린지의 태그들

    public ChallengeInviteListResponse(
            String inviterNickname,
            long challengeId,
            String challengeTitle,
            int participantCount,
            long remainingDays,
            List<String> tags
    ) {
        this.inviterNickname = inviterNickname;
        this.challengeId = challengeId;
        this.challengeTitle = challengeTitle;
        this.participantCount = participantCount;
        this.remainingDays = remainingDays;
        this.tags = tags;
    }
}
