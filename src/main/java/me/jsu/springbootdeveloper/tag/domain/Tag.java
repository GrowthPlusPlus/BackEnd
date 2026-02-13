// 정성우

package me.jsu.springbootdeveloper.tag.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    /*
        태그가 삭제되면 유저나 챌린지에 저장된 태그도 자동으로 삭제된다.
     */
    @OneToMany(mappedBy = "tag", cascade = CascadeType.REMOVE)
    private List<TagToUser> tagToUsers;

    @OneToMany(mappedBy = "tag", cascade = CascadeType.REMOVE)
    private List<TagToChallenge> tagToChallenges;

    @Builder
    public Tag(String tagName, TagCategory tagCategory) {
        this.tagName = tagName;
        this.tagCategory = tagCategory;
    }
}
