// 정성우

package me.jsu.springbootdeveloper.dto.article;

import lombok.Builder;
import lombok.Getter;

/*
    이미지 주소, 이미지 id를 갖고 있는 dto이다.
    이미지 id를 함께 전달하여 이미지 삭제 시 사용하도록 한다.
 */
@Getter
@Builder
public class ArticleImageResponse {
    private Long imageId;
    private String imageUrl;
}
