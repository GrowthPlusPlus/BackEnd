// 정성우

package me.jsu.springbootdeveloper.post.dto.article;

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
    private Long postId;  // 게시글 수정을 위해 필요하다.
    private String challengeTitle;
    private int totalSuccessDays;
    private String title;
    private String content;
    private List<ArticleImageResponse> images;
    private String userNickname;
    private String userImageUrl;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private boolean isEdited;

    private long likeNumber;
    private long commentNumber;
    boolean liked;
}
