// 정성우

package me.jsu.springbootdeveloper.dto.mainHome;

import lombok.Builder;
import lombok.Getter;

/*
    유저의 챌린지 별 정보가 들어가있는 dto이다.
 */
@Builder
@Getter
public class MyChallenge {
    private Long challengeId;
    private String title;
    private String content;  // 챌린지 설명이다.
    private int maxParticipantNumber;
    private int participantNumber;
    private long duringDate;  // 챌린지 경과일이다.
    private boolean doIt;  // 해당 챌린지를 오늘 수행했는지에 관한 불린 값이다.
    private boolean warning;  // 오늘 챌린지를 수행하지 않으면 실패하는지에 관한 불린 값이다.
}
