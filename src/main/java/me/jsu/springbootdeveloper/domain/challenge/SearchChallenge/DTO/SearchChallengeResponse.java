package me.jsu.springbootdeveloper.domain.challenge.SearchChallenge.DTO;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.tag.TagToChallenge;
import me.jsu.springbootdeveloper.dto.tag.TagResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
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