// 정성우

package me.jsu.springbootdeveloper.challenge.dto.ranking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
    랭킹 점수를 보여주기 위한 정보를 보내준다.
 */
@Getter
@Setter
@AllArgsConstructor
public class RankingResponse {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Integer totalSuccessDays;  // 총 달성 일수
    private Integer currentStreakDate;  // 현재 연속 달성 일수
    private int rank;
}
