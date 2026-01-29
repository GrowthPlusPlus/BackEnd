// 정성우

package me.jsu.springbootdeveloper.view.dto;

import lombok.Getter;

import java.time.LocalDate;

/*
    사용자가 올린 최신 인증글을 불러오는 dto이다.
 */
@Getter
public class ChallengeCalendarPostResponse {
    private final Long postId;
    private final LocalDate postDate;
    private final String imageUrl;
    private final String content;  // 제목 대신 내용을 불러온다.

    public ChallengeCalendarPostResponse(Long postId, LocalDate postDate, String imageUrl, String content) {
        this.postId = postId;
        this.postDate = postDate;
        this.imageUrl= imageUrl;
        this.content = content;
    }
}