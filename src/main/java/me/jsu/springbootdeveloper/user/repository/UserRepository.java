// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/*
    유저 정보를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 User, 엔티티의 pk 타입은 Long이다.
    findByAll 검색 방식 이외에도 유저 이메일로 조회가 가능하다.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);  // 이메일 정보로 사용자 정보를 가져올 수 있게 하는 쿼리문을 작성 가능하게 한다.
    Optional<User> findByNickname(String nickname);  // 닉네임 정보로 사용자 정보를 가져온다.
    boolean existsByNickname(String nickname);  // 닉네임이 이미 존재하는지 검사한다.

    //유저 닉네임 검색 시 해당 유저와 나의 관계 보여주기
    @Query("""
    SELECT u.id AS id, u.nickname AS nickname, up.imageUrl AS profileImage,
           CASE 
               WHEN (SELECT COUNT(f) FROM Friendship f 
                     WHERE (f.user1.id = u.id AND f.user2.id = :myId) 
                        OR (f.user1.id = :myId AND f.user2.id = u.id)) > 0 THEN 'FRIEND'
               WHEN (SELECT COUNT(fr) FROM FriendRequest fr 
                     WHERE fr.fromUser.id = :myId AND fr.toUser.id = u.id 
                       AND fr.status = 'REQUESTED') > 0 THEN 'PENDING_SENT'
               WHEN (SELECT COUNT(fr) FROM FriendRequest fr 
                     WHERE fr.fromUser.id = u.id AND fr.toUser.id = :myId 
                       AND fr.status = 'REQUESTED') > 0 THEN 'PENDING_RECEIVED'
               ELSE 'NONE'
           END AS status
    FROM User u
    LEFT JOIN u.userProfilePicture up
    WHERE u.nickname LIKE %:nickname%
      AND u.id <> :myId
""")
    List<UserFriendshipSearchProjection> searchAllUsersWithRelationship(
            @Param("nickname") String nickname,
            @Param("myId") Long myId);
}

