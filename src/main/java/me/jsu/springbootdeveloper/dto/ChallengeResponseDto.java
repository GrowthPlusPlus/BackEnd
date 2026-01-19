package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class ChallengeResponseDto {
    private Long id;                // 생성된 챌린지 식별자
    private String title;
    private String content;
    private String creatorNickname; // User 객체 대신 닉네임만 노출 (보안)
    private int participantNumber;   // 현재 참여 인원
    private int maxParticipantNumber;
    private LocalDate startDate;
    private LocalDate endDate;       // 서비스에서 계산된 종료일
    private int requiredWeeklyCount;
    private LocalDateTime createdAt; // 생성 시간 (Auditing)
    private ChallengeVisibility challengeVisibility;

    // Entity -> DTO 변환 생성자
    public ChallengeResponseDto(Challenge challenge) {
        this.id = challenge.getId();
        this.title = challenge.getTitle();
        this.content = challenge.getContent();
        this.creatorNickname = challenge.getUser().getNickname(); // User 엔티티에서 가져옴
        this.participantNumber = challenge.getParticipantNumber();
        this.maxParticipantNumber = challenge.getMaxParticipantNumber();
        this.startDate = challenge.getStartDate();
        this.endDate = challenge.getEndDate();
        this.requiredWeeklyCount = challenge.getRequiredWeeklyCount();
        this.createdAt = challenge.getCreatedAt();
        this.challengeVisibility = challenge.getChallengeVisibility();
    }
}