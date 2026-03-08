// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    유저의 프로필 사진을 저장하는 DB이다.
 */
public interface UserProfilePictureRepository extends JpaRepository<UserProfilePicture, Long> {
    Optional<UserProfilePicture> findByUser_Id(Long userId);  // 유저 id로 유저 프로필 사진을 검색한다.

    // 유저 id로 해당 유저의 image url을 가져온다.
    @Query(""" 
            SELECT up.imageUrl
            FROM User u
            LEFT JOIN u.userProfilePicture up
            WHERE u.id = :userId
            """)
    Optional<String> findImageUrlByUserId(@Param("userId") Long userId);

    // 유저 id 리스트로 프로필 사진 정보를 가져온다.
    @Query("""
        select p
        from UserProfilePicture p
        where p.user.id in :userIds
    """)
    List<UserProfilePicture> findByUserIds(@Param("userIds") List<Long> userIds);
}
