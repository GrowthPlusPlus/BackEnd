// 정성우

package me.jsu.springbootdeveloper.challenge.dto.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/*
    캘린더 공유 html에 정보를 넘겨주기 위한 dto이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareImageRenderRequest {
    // 상단 뱃지 (예: "30일")
    private String badgeText;

    // 챌린지 제목
    private String title;

    // 이번 달 성공 횟수
    private int monthlyDone;

    // 이번 달 목표 횟수
    private int monthlyTotal;

    // 챌린지 시작일
    private String startDate;

    // 총 성공 횟수
    private int totalDone;

    // 이번 달 1일의 요일
    // 0 = 일요일
    // 1 = 월요일
    // ...
    // 6 = 토요일
    private int startDayOfWeek;

    // 캘린더 날짜 정보
    private List<DayInfo> days;
}
