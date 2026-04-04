// 정성우

package me.jsu.springbootdeveloper.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/*
    이번 주와 저번 주의 챌린지 성공 추이를 반환하는 dto이다.
 */
@Getter
@Builder
public class DailyGraphResponse {
    List<Integer> thisWeek;
    List<Integer> lastWeek;
}
