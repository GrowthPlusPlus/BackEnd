// 정성우

package me.jsu.springbootdeveloper.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/*
    AI 챌린지 추천(RAG) 결과를 탐색 탭 카드 UI 형태로 내려주기 위한 응답 DTO이다.
 */
@Getter
@AllArgsConstructor
public class ChallengeDiscoveryRecommendItemResponse {
    // 카드에 표시할 챌린지 이름이다.
    private final String challengeName;
    // 현재 참여 중인 인원 수이다.
    private final int participantCount;
    // 종료일까지 남은 일수이다. 형식은 항상 D- 뒤 세 자리 숫자(예: D-005)이다.
    private final String remainingDays;
    // 해당 챌린지에 연결된 태그 이름 목록이다.
    private final List<String> tags;
    // 상세 화면 이동 등에 사용할 챌린지 PK이다.
    private final Long challengeId;
}
