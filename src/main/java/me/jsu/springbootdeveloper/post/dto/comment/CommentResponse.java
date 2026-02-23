// 정성우

package me.jsu.springbootdeveloper.post.dto.comment;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/*
    게시글에 있는 댓글 정보를 가지고 있는 dto이다.
 */
@Getter
@Builder
public class CommentResponse {
    private Long commentId;  // 댓글 수정, 삭제 등을 처리하기 위해 미리 id 정보까지 보낸다.
    private String userNickname;
    private String userPicture;
    private String contents;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private boolean isEdited;
    private boolean mine;
}
