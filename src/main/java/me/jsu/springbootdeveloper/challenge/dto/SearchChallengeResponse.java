package me.jsu.springbootdeveloper.challenge.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;

import java.util.List;

@Getter
public class SearchChallengeResponse {
    //챌린지 이름
    private String title;
    //현재 참여자 수
    private int participantNumber;
    //인증 빈도
    private int requiredWeeklyCount;
    //완료까지 남은 날짜 (D-000) -> 나중에 구현
    //인증 사진 필수 여부
    private boolean photoRequired;
    //태그들
    private List<TagResponse> tags;
    //인증률 -> 나중에 구현

    public SearchChallengeResponse(Challenge challenge, List<TagResponse> tags) {

        this.title = challenge.getTitle();
        this.participantNumber = challenge.getParticipantNumber();
        this.requiredWeeklyCount = challenge.getRequiredWeeklyCount();
        this.photoRequired = challenge.isPhotoRequired();
        this.tags = tags;
    }
}