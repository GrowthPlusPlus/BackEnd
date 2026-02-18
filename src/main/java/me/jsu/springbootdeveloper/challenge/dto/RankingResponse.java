// 정성우

package me.jsu.springbootdeveloper.challenge.dto;

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
    private Double rankingScore;
    private int rank;
}
