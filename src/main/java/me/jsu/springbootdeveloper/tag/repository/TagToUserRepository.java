// 정성우

package me.jsu.springbootdeveloper.tag.repository;

import me.jsu.springbootdeveloper.tag.domain.TagToUser;
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

    void deleteByUser_IdAndTag_IdIn(Long userId, List<Long> tagIds);  // 유저의 관심 태그를 id 리스트로 삭제한다.

    List<TagToUser> findAllByUser_Id(Long userId);  // 유저의 기존 태그를 검색한다.
}
