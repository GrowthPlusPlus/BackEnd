// 정성우

package me.jsu.springbootdeveloper.user.dto;

import lombok.Builder;
import lombok.Getter;

/*
    유저의 태그별 챌린지 성공 횟수 정보를 담고있는 dto이다.
 */
@Getter
@Builder
public class TagGraphResponse {
    String tagName;
    int count;
}
