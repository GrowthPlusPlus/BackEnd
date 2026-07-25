// 정성우

package me.jsu.springbootdeveloper.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
    맞춤형 코칭의 결과를 잘한 점, 개선할 점, 추천 행동으로 분리하여 반환하는 dto이다.
 */
@Getter
@AllArgsConstructor
public class CoachResponse {
    private String goodPoint;
    private String improvement;
    private String recommendation;
}
