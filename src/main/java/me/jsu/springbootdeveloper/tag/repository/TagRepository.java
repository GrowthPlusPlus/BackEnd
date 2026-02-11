// 정성우

package me.jsu.springbootdeveloper.tag.repository;

import me.jsu.springbootdeveloper.tag.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    유저와 챌린지의 tag를 저장하는 DB이다.
 */
public interface TagRepository extends JpaRepository<Tag, Long> {
    boolean existsByTagName(String tagName);
    List<Tag> findAllByOrderByTagCategoryAscTagNameAsc();
}
