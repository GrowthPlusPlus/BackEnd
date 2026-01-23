package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.FriendListResponse;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse_Sent;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse_Received;
import me.jsu.springbootdeveloper.repository.user.UserRepository;
import me.jsu.springbootdeveloper.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/friend")
public class FriendRequestController {
    private final FriendService friendService;
    private final UserRepository userRepository;

    //친구 신청 보내기
    @PostMapping("/request/{toUserNickName}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable String toUserNickName,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 버튼 누르면 실행되는 로직
        friendService.sendFriendRequest(userDetails.getUsername(), toUserNickName);

        return ResponseEntity.ok().build();
    }

    //받은 요청 목록 조회
    @GetMapping("/request/received")
    public ResponseEntity<List<FriendRequestResponse_Received>> getReceivedRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FriendRequestResponse_Received> responses =
                friendService.getReceivedRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }

    //친구 요청 수락
    @PatchMapping("/request/{requestId}/accept")
    public ResponseEntity<String> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.acceptFriendRequest(requestId, userDetails.getUsername());

        return ResponseEntity.ok("친구 신청을 수락했습니다.");
    }

    //친구 요청 거절
    @PatchMapping("/request/{requestId}/reject")
    public ResponseEntity<String> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.rejectOrCancelRequest(requestId, userDetails.getUsername(), true);

        return ResponseEntity.ok("친구 신청을 거절했습니다.");
    }
    //보낸 요청 목록 조회
    @GetMapping("/request/sent")
    public ResponseEntity<List<FriendRequestResponse_Sent>> getSentRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        // getReceivedRequests 대신 getSentRequests 호출
        List<FriendRequestResponse_Sent> responses =
                friendService.getSentRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }
    //보낸 요청 -> 요청 취소
    @PatchMapping("/request/sent/{requestId}/cancel")
    public ResponseEntity<String> cancelFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.rejectOrCancelRequest(requestId, userDetails.getUsername(), false);

        return ResponseEntity.ok("친구 신청을 취소했습니다.");
    }
    // 친구 삭제
    @DeleteMapping("/delete/{friendNickname}")
    public ResponseEntity<Void> deleteFriend(
            @PathVariable String friendNickname,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.deleteFriend(userDetails.getUsername(), friendNickname);

        return ResponseEntity.noContent().build();
    }

    //친구 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<FriendListResponse>> getFriendList(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FriendListResponse> friends = friendService.getFriendList(userDetails.getUsername());

        return ResponseEntity.ok(friends);
    }

}