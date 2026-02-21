// 정성우

package me.jsu.springbootdeveloper.user.repository;

import me.jsu.springbootdeveloper.user.domain.FriendRequest;
import me.jsu.springbootdeveloper.user.enumType.FriendRequestStatus;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/*
    친구 요청을 저장하는 DB이다.
 */
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    // 유저 id로 받은 친구요청 중 현재 유효한 요청들의 리스트를 검색한다. n+1 조회문제를 위해 fetch join을 사용한다.
    @Query("""
        select fr
        from FriendRequest fr
        join fetch fr.fromUser fu
        left join fetch fu.userProfilePicture upp
        where fr.toUser.id = :toUserId
          and fr.status = :status
    """)
    List<FriendRequest> findAllByToUser_IdAndStatus(
            @Param("toUserId") Long toUserId,
            @Param("status") FriendRequestStatus status
    );

    // 유저 id로 보낸 친구요청 중 현재 유효한 요청들의 리스트를 검색한다. n+1 조회문제를 위해 fetch join을 사용한다.
    @Query("""
        select fr
        from FriendRequest fr
        join fetch fr.toUser tu
        left join fetch tu.userProfilePicture upp
        where fr.fromUser.id = :fromUserId
          and fr.status = :status
    """)
    List<FriendRequest> findAllByFromUser_IdAndStatus(
            @Param("fromUserId") Long fromUserId,
            @Param("status") FriendRequestStatus status
    );

    //fromuser 와 touser 객체를 넣어 검색한다
    FriendRequest findByFromUserAndToUser(User fromUser, User toUser);

    /*현재 유효한 친구 요청이 존재하는 상태에서 중복된 친구 요청을 보내는지 검사한다.
        즉 A -> B 요청이 있는 상태에서 B -> A 요청을 보내고 있지 않은지 확인한다.
     */
    @Query("""
        select case when count(fr) > 0 then true else false end
        from FriendRequest fr
        where fr.fromUser = :fromUser
          and fr.toUser = :toUser
          and fr.status = 'REQUESTED'
    """)
    boolean existsRequested(@Param("fromUser") User fromUser, @Param("toUser") User toUser);
}
