// 정성우

package me.jsu.springbootdeveloper.dto.challengeCalendar;

import lombok.Getter;
import java.time.LocalDate;

/*
    챌린지 달력에서 챌린지를 수행한 날짜의 사진을 가져오기 위한 dto이다.
    사진을 클릭하면 게시글로 이동할 수 있도록 게시글 id도 존재한다.
    게시글에 인증 사진이 없는 경우 null로 처리한다.
 */
@Getter
public class ChallengeCalendarPhotoResponse {
    private final Long postId;        // 달력 칸 클릭 시 상세로 이동/조회에 필요한 정보이다.
    private final LocalDate postDate;  // 날짜
    private final String imageUrl;    // 대표 사진(썸네일) URL

    public ChallengeCalendarPhotoResponse(Long postId, LocalDate postDate, String imageUrl) {
        this.postId = postId;
        this.postDate = postDate;
        this.imageUrl = imageUrl;
    }
}
