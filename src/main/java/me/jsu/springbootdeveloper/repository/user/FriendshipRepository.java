// 정성우

package me.jsu.springbootdeveloper.repository.user;

import me.jsu.springbootdeveloper.domain.user.Friendship;
import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    친구 관계를 저장하는 DB이다.
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // :userId는 나중에 자바에서 값을 넣기 위해 자동으로 넣어주는 값이다.
    @Query("""
        select f
        from Friendship f
        where f.user1.id = :userId
           or f.user2.id = :userId
    """)
    List<Friendship> findAllByUserId(@Param("userId") Long userId);  // 유저 id로 해당 유저의 모든 친구 목록을 검색한다.

    /*
        이미 친구로 등록되어있는지 검사해준다. 해당 로직을 수행하기 전에 서비스 레벨에서 user1Id < user2Id 의 순서가 정해져야 한다.
        해당 검사는 조회용 api가 아닌 도메인 규칙으로 존재 여부를 검사하기 때문에 user id가 아닌 객체로 비교한다.
     */
    boolean existsByUser1AndUser2(User user1, User user2);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.user1 = :user AND f.user2 = :friend) OR (f.user1 = :friend AND f.user2 = :user)")
    void deleteFriendship(@Param("user") User user, @Param("friend") User friend);

}
