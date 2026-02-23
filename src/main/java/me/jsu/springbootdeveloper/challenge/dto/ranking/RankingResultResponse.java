// 정성우

package me.jsu.springbootdeveloper.challenge.dto.ranking;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/*
    전체 랭킹 점수를 보여주기 위한 dto이다. 자신의 정보도 포함되어 있다.
 */
@Getter
@AllArgsConstructor
public class RankingResultResponse {
    private List<RankingResponse> topRankings;  // 100명까지의 랭킹
    private RankingResponse myRanking;
}