// 정성우

package me.jsu.springbootdeveloper.domain.tag;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.jsu.springbootdeveloper.domain.notification.NotificationType;

/*
    유저와 챌린지에 들어갈 태그를 저장하는 도메인 클래스이다.
 */
@Table(name = "tag")
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_name", nullable = false, unique = true)  // 태그 이름 중복 방지를 위해 유니크 설정을 한다.
    private String tagName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_category", nullable = false)
    private TagCategory tagCategory;  // 태그 타입을 지정한다.

    @Builder
    public Tag(String tagName, TagCategory tagCategory) {
        this.tagName = tagName;
        this.tagCategory = tagCategory;
    }
}
