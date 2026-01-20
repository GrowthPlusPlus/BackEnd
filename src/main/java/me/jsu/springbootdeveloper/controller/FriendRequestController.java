package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse;
import me.jsu.springbootdeveloper.repository.user.UserRepository;
import me.jsu.springbootdeveloper.service.FriendRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/friend")
public class FriendRequestController {
    private final FriendRequestService friendRequestService;
    private final UserRepository userRepository;

    //친구 신청 보내기
    @PostMapping("/request/{toUserNickName}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable String toUserNickName,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 버튼 누르면 실행되는 로직
        friendRequestService.sendFriendRequest(userDetails.getUsername(), toUserNickName);

        return ResponseEntity.ok().build();
    }

    //받은 요청 목록 조회
    @GetMapping("/request/received")
    public ResponseEntity<List<FriendRequestResponse>> getReceivedRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FriendRequestResponse> responses =
                friendRequestService.getReceivedRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }

    //친구 요청 수락
    @PatchMapping("/request/{requestId}/accept")
    public ResponseEntity<String> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendRequestService.acceptFriendRequest(requestId, userDetails.getUsername());

        return ResponseEntity.ok("친구 신청을 수락했습니다.");
    }

    //친구 요청 거절
    @PatchMapping("/request/{requestId}/reject")
    public ResponseEntity<String> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendRequestService.rejectOrCancelRequest(requestId, userDetails.getUsername(), true);

        return ResponseEntity.ok("친구 신청을 거절했습니다.");
    }
    //보낸 요청 목록 조회
    @GetMapping("/request/sent")
    public ResponseEntity<List<FriendRequestResponse>> getSentRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        // getReceivedRequests 대신 getSentRequests 호출
        List<FriendRequestResponse> responses =
                friendRequestService.getSentRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }
    //보낸 요청 -> 요청 취소
    @PatchMapping("/request/sent/{requestId}/cancel")
    public ResponseEntity<String> cancelFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendRequestService.rejectOrCancelRequest(requestId, userDetails.getUsername(), false);

        return ResponseEntity.ok("친구 신청을 취소했습니다.");
    }
}