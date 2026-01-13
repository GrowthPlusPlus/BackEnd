// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.PostPicture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/*
    게시글의 사진을 저장하는 DB이다.
 */
public interface PostPictureRepository extends JpaRepository<PostPicture, Long> {
    List<PostPicture> findAllByArticleId(Long articleId);  // 게시글의 id로 모든 사진을 검색한다.
    long countByArticleId(Long articleId);  // 게시글 id로 게시글에 등록된 사진 개수를 구한다. 게시글 당 사진 개수 제한에 활용 가능하다.
}
