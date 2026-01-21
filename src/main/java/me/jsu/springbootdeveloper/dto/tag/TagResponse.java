// 정성우

package me.jsu.springbootdeveloper.dto.tag;

import lombok.Getter;
import me.jsu.springbootdeveloper.domain.tag.TagCategory;

/*
    태그를 클라이언트에게 반환하기 위해 사용하는 dto이다.
 */
@Getter
public class TagResponse {
    private final Long tagId;  // 유저가 태그를 선택할 경우, 해당 값으로 처리하기 위해 id까지 클라이언트에게 전달한다.
    private final String tag;
    private final TagCategory tagCategory;

    public TagResponse(Long tagId, String tag, TagCategory tagCategory) {
        this.tagId = tagId;
        this.tag = tag;
        this.tagCategory = tagCategory;
    }
}
