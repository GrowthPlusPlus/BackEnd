// 정성우

package me.jsu.springbootdeveloper.repository.tag;

import me.jsu.springbootdeveloper.domain.tag.TagToUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    유저와 tag를 저장하는 DB이다.
 */
public interface TagToUserRepository extends JpaRepository<TagToUser, Long> {
    // 태그 이름까지 한 번에 가져오기 위해 쿼리문을 사용한다.
    @Query(""" 
        select ttu
        from TagToUser ttu
        join fetch ttu.tag
        where ttu.user.id = :userId
    """)
    List<TagToUser> findWithTagByUserId(@Param("userId") Long userId);  // 유저 id로 유저와 관련된 태그를 검색한다.

    boolean existsByUser_IdAndTag_Id(Long userId, Long tagId);  // 태그 id와 유저 id로 해당 유저가 태그를 가지고 있는지 확인한다.
}
