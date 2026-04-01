// 정성우

package me.jsu.springbootdeveloper.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/*
    유저의 연간 활동(잔디) 정보를 반환하기 위한 클래스이다.
 */
@Getter
@Builder
public class UserYearlyActivityResponse {
    int[] activity;
}
