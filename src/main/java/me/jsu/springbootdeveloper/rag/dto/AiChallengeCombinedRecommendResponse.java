// 정성우

package me.jsu.springbootdeveloper.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.dto.basic.SearchChallengeResponse;

import java.util.List;

/*
    탐색 탭 AI 챌린지 추천 합본 응답 DTO이다.
    배너용 서술형 요약과, 카드 목록(각 카드별 추천 이유 포함)을 한 번에 담는다.
 */
@Getter
@AllArgsConstructor
public class AiChallengeCombinedRecommendResponse {

    /*
        화면 상단 배너에 보여줄 서술형 요약이다.
        (관심 태그·참여 이력 등을 바탕으로 한 전체 안내 문구)
     */
    private final String summary;

    /*
        추천된 챌린지 카드 목록이다.
        각 항목은 카드 데이터와 해당 챌린지의 추천 이유를 함께 가진다.
     */
    private final List<AiChallengeCombinedRecommendItemResponse> challenges;

    /*
        합본 응답 안의 개별 추천 카드 항목이다.
        기존 탐색 카드(SearchChallengeResponse)에
        챌린지 소개(content)·추천 이유를 붙인 형태이다.
     */
    @Getter
    @AllArgsConstructor
    public static class AiChallengeCombinedRecommendItemResponse {

        /*
            챌린지 카드에 표시할 기본 정보이다.
            (제목, 참여 인원, 잔여일, 태그 등)
         */
        private final SearchChallengeResponse challenge;

        /*
            챌린지 설명(소개)이다.
            AI가 생성하지 않고 Challenge.content 필드 값을 그대로 넣는다.
         */
        private final String content;

        /*
            해당 챌린지를 추천한 이유이다.
            카드의 "추천 이유 보기"에 사용한다.
         */
        private final String recommendReason;
    }
}
