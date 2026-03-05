// 정성우

package me.jsu.springbootdeveloper.post.repository;

import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/*
    검증된 사진을 임시로 저장하는 DB이다.
 */
public interface PostPictureTemporaryRepository extends JpaRepository<PostPictureTemporary, Long> {
    // 만료된 임시 파일들을 찾는 쿼리문이다.
    @Query("""
        select t
        from PostPictureTemporary t
        where t.createdAt < :threshold
    """)
    List<PostPictureTemporary> findExpired(@Param("threshold") Instant threshold);
}
