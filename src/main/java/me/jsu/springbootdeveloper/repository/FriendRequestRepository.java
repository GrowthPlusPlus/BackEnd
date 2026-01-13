// 정성우

package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.FriendRequest;
import me.jsu.springbootdeveloper.domain.FriendRequestStatus;
import me.jsu.springbootdeveloper.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
    친구 요청을 저장하는 DB이다.
 */
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {
    // 유저 id로 받은 친구요청 중 현재 유효한 요청들의 리스트를 검색한다.
    List<FriendRequest> findAllByToUserIdAndStatus(Long toUserId, FriendRequestStatus status);

    // 유저 id로 보낸 친구요청 중 현재 유효한 요청들의 리스트를 검색한다.
    List<FriendRequest> findAllByFromUserIdAndStatus(Long fromUserId, FriendRequestStatus status);

    /*
        현재 유효한 친구 요청이 존재하는 상태에서 중복된 친구 요청을 보내는지 검사한다.
        즉 A -> B 요청이 있는 상태에서 B -> A 요청을 보내고 있지 않은지 확인한다.
     */
    boolean existsByFromUserAndToUserAndStatusRequested(User fromUser, User toUser);
}
