// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

/*
    게시글을 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 Article, 엔티티의 pk 타입은 Long이다.
 */
public interface PostRepository extends JpaRepository<Article, Long> {
}
