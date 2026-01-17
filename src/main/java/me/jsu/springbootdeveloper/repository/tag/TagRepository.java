// 정성우

package me.jsu.springbootdeveloper.repository.tag;

import me.jsu.springbootdeveloper.domain.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

/*
    유저와 챌린지의 tag를 저장하는 DB이다.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
}
