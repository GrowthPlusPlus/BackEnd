// 정성우

package me.jsu.springbootdeveloper.challenge.dto.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
    캘린더 공유 html에 넘겨주기 위한 정보 중, 달력의 날짜 정보를 가진 dto이다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayInfo {
    // 날짜
    private int date;

    // empty, done, photo
    private String status;

    // 인증 사진 URL (photo일 때만 사용)
    private String photoUrl;
}
