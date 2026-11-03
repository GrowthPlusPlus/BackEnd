package me.jsu.springbootdeveloper.challenge.dto.basic;



import lombok.Getter;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;

import me.jsu.springbootdeveloper.challenge.service.ChallengeUtils;

import me.jsu.springbootdeveloper.tag.dto.TagResponse;



import java.time.LocalDate;

import java.util.List;



@Getter

public class SearchChallengeResponse {

    //챌린지 id

    private long challengeId;

    //챌린지 이름

    private String title;

    //현재 참여자 수

    private int participantNumber;

    //인증 빈도

    private int requiredWeeklyCount;

    //완료까지 남은 날짜 (D-day)

    private long remainingDays;

    //인증 사진 필수 여부

    private boolean photoRequired;

    //태그들

    private List<TagResponse> tags;

    //현재 사용자가 챌린지 참여 중인지 여부
    private boolean join;

    //나가거나 강퇴된 적이 있어 다시 참여할 수 없는지 여부
    private boolean joinedBefore;

    //인증률 -> 나중에 구현



    /*
        참여 여부를 넘기지 않는 호출부용 생성자이다.
        join과 joinedBefore는 false로 둔다.

        @param challenge 검색된 챌린지이다.
        @param tags 챌린지에 연결된 태그 목록이다.
     */
    public SearchChallengeResponse(Challenge challenge, List<TagResponse> tags) {
        this(challenge, tags, false, false);
    }

    /*
        검색 결과와 현재 사용자의 참여 여부를 함께 담는다.
        이탈 이력을 넘기지 않으면 joinedBefore는 false이다.

        @param challenge 검색된 챌린지이다.
        @param tags 챌린지에 연결된 태그 목록이다.
        @param join 현재 사용자가 해당 챌린지를 진행 중이면 true이다.
     */
    public SearchChallengeResponse(Challenge challenge, List<TagResponse> tags, boolean join) {
        this(challenge, tags, join, false);
    }

    /*
        검색 결과, 현재 참여 여부, 과거 이탈 여부를 함께 담는다.

        @param challenge 검색된 챌린지이다.
        @param tags 챌린지에 연결된 태그 목록이다.
        @param join 현재 사용자가 해당 챌린지를 진행 중이면 true이다.
        @param joinedBefore 나가거나 강퇴된 적이 있으면 true이다.
     */
    public SearchChallengeResponse(
            Challenge challenge,
            List<TagResponse> tags,
            boolean join,
            boolean joinedBefore
    ) {
        this.challengeId = challenge.getId();
        this.title = challenge.getTitle();
        this.participantNumber = challenge.getParticipantNumber();
        this.requiredWeeklyCount = challenge.getRequiredWeeklyCount();
        // 오늘 기준 종료일까지 남은 일수를 계산한다. (종료일 포함, 과거일은 0)
        this.remainingDays = ChallengeUtils.calculateDDay(LocalDate.now(), challenge.getEndDate());
        this.photoRequired = challenge.isPhotoRequired();
        this.tags = tags;
        this.join = join;
        this.joinedBefore = joinedBefore;
    }

}


