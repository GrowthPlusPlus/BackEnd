// 정성우

package me.jsu.springbootdeveloper.repository.user;

import me.jsu.springbootdeveloper.domain.user.UserProfilePicture;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/*
    유저의 프로필 사진을 저장하는 DB이다.
 */
public interface UserProfilePictureRepository extends JpaRepository<UserProfilePicture, Long> {
    Optional<UserProfilePicture> findByUser_Id(Long userId);  // 유저 id로 유저 프로필 사진을 검색한다.
}
