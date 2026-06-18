// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Builder;
import lombok.Getter;
import me.jsu.springbootdeveloper.view.domain.DayStatus;

import java.time.LocalDate;

/*
    mainHome에서 요청받은 일을 기준으로, 주간 동안의 날짜별 상태를 저장하는 dto이다.
    1. 빨간색 - 해당 날에 챌린지를 수행해서 하나라도 실패한 챌린지가 있는 경우
    2. 초록색 - 해당 날에 하나라도 챌린지를 수행한 경우
    3. 회색 - 해당 날에 아무런 챌린지를 수행하지 않은 경우
 */
@Builder
@Getter
public class WeekDayStatusResponse {
    private LocalDate date;
    private DayStatus status;
}