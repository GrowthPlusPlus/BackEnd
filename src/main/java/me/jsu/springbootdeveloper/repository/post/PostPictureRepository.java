// 정성우

package me.jsu.springbootdeveloper.repository.post;

import me.jsu.springbootdeveloper.domain.post.PostPicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    게시글의 사진을 저장하는 DB이다.
 */
public interface PostPictureRepository extends JpaRepository<PostPicture, Long> {
    List<PostPicture> findAllByArticle_Id(Long articleId);  // 게시글의 id로 모든 사진을 검색한다.
    long countByArticle_Id(Long articleId);  // 게시글 id로 게시글에 등록된 사진 개수를 구한다. 게시글 당 사진 개수 제한에 활용 가능하다.

    // 게시글 id로 모든 사진을 검색한다. PostPicture 객체가 아닌, 이미지 링크 형태로 바로 반환한다.
    @Query("""
        select pp.imageUrl
        from PostPicture pp
        where pp.article.id = :articleId
    """)
    List<String> findImageUrlsByPostId(@Param("articleId") Long postId);

    // 게시글 id List로 사진을 검색한다. 위와는 다르게 단체 조회이다.
    @Query("""
        select pi.article.id, pi.id, pi.imageUrl
        from PostPicture pi
        where pi.article.id in :postIds
    """)
    List<Object[]> findImagesByPostIds(@Param("postIds") List<Long> postIds);
}
