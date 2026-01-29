// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.challenge.enumType.Status;

import java.time.LocalDate;

/*
    나의 챌린지 완료, 실패 값을 저장하는 dto이다.
 */
@Getter
@Builder
public class MyChallengeSuccessOrFailResponse {
    private Long challengeId;
    private String title;
    private long duringDate;  // 챌린지를 수행한 일수이다.
    private LocalDate endDate;  // 챌린지를 완수하거나, 실패한 날짜이다.
    private int achievementRate;
    private Status status;
}
