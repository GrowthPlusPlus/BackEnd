// 정성우

package me.jsu.springbootdeveloper.dto.article;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/*
    DB에 조회한 게시글의 상세 정보를 가진 dto클래스이다.
 */
@Getter
@Builder
public class ArticleDetailResponse {
    private Long postId;
    private String challengeTitle;
    private int totalSuccessDays;
    private String content;
    private List<String> articleImageUrl;
    private String userNickname;
    private String userImageUrl;
    private LocalDateTime createdAt;

    private long likeNumber;
    private long commentNumber;
    boolean liked;
}
