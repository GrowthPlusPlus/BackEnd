// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.enumType.Role;
import me.jsu.springbootdeveloper.user.enumType.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
    Optional<User> findByFcmToken(String fcmToken);  // FCM 토큰으로 사용자를 찾는다. 만료된 토큰 삭제 시 사용한다.
    boolean existsByEmail(String email);  // 이메일이 이미 존재하는지 검사한다.

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

    // 회원탈퇴한 유저 중, 30일이 지난 유저를 검색하는 쿼리문이다.
    @Query("""
        select u
        from User u
        where u.userStatus = :status
          and u.deletedAt < :threshold
    """)
    List<User> findUsersToHardDelete(
            @Param("status") UserStatus status,
            @Param("threshold") LocalDateTime threshold
    );

    // 유저의 권한과 상태로 유저 목록을 조회하는 쿼리문이다.
    List<User> findAllByRoleAndUserStatus(Role role, UserStatus userStatus);
}

