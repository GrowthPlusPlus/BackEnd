package me.jsu.springbootdeveloper.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.dto.FriendListResponse;
import me.jsu.springbootdeveloper.user.dto.FriendRequestResponse_Sent;
import me.jsu.springbootdeveloper.user.dto.FriendRequestResponse_Received;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/friend")
public class FriendController {
    private final FriendService friendService;
    private final UserRepository userRepository;

    //친구 신청 보내기
    @Operation(
            summary = "친구 신청 보내기",
            description = """
        친구 신청을 보낸다.
        요청: nickname
        ex) POST /api/users/friend/request/유저닉네임
        """
    )
    @PostMapping("/request/{toUserNickName}")
    public ResponseEntity<Void> sendFriendRequest(
            @PathVariable String toUserNickName,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 버튼 누르면 실행되는 로직
        friendService.sendFriendRequest(userDetails.getUsername(), toUserNickName);

        return ResponseEntity.ok().build();
    }

    //받은 요청 목록 조회
    @Operation(
            summary = "내가 받은 친구 신청 목록 조회하기",
            description = """
        ex) GET /api/users/friend/request/received
        """
    )
    @GetMapping("/request/received")
    public ResponseEntity<List<FriendRequestResponse_Received>> getReceivedRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FriendRequestResponse_Received> responses =
                friendService.getReceivedRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }

    //친구 요청 수락
    @Operation(
            summary = "친구 신청 수락하기",
            description = """
        """
    )
    @PatchMapping("/request/accept/{requestId}")
    public ResponseEntity<String> acceptRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.acceptFriendRequest(requestId, userDetails.getUsername());

        return ResponseEntity.ok("친구 신청을 수락했습니다.");
    }

    //친구 요청 거절
    @Operation(
            summary = "친구 신청 거절하기",
            description = """
        """
    )
    @PatchMapping("/request/reject/{requestId}")
    public ResponseEntity<String> rejectRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.rejectOrCancelRequest(requestId, userDetails.getUsername(), true);

        return ResponseEntity.ok("친구 신청을 거절했습니다.");
    }
    //보낸 요청 목록 조회
    @Operation(
            summary = "내가 보낸 친구 신청 목록 조회하기",
            description = """
        """
    )
    @GetMapping("/request/sent")
    public ResponseEntity<List<FriendRequestResponse_Sent>> getSentRequests(
            @AuthenticationPrincipal UserDetails userDetails) {

        // getReceivedRequests 대신 getSentRequests 호출
        List<FriendRequestResponse_Sent> responses =
                friendService.getSentRequests(userDetails.getUsername());

        return ResponseEntity.ok(responses);
    }
    //보낸 요청 -> 요청 취소
    @Operation(
            summary = "내가 보낸 친구 신청 취소하기",
            description = """
        """
    )
    @PatchMapping("/request/sent/cancel/{requestId}")
    public ResponseEntity<String> cancelFriendRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.rejectOrCancelRequest(requestId, userDetails.getUsername(), false);

        return ResponseEntity.ok("친구 신청을 취소했습니다.");
    }
    // 친구 삭제
    @Operation(
            summary = "친구 삭제하기",
            description = """
        """
    )
    @DeleteMapping("/delete/{friendNickname}")
    public ResponseEntity<Void> deleteFriend(
            @PathVariable String friendNickname,
            @AuthenticationPrincipal UserDetails userDetails) {

        friendService.deleteFriend(userDetails.getUsername(), friendNickname);

        return ResponseEntity.noContent().build();
    }

    //친구 목록 조회
    @Operation(
            summary = "친구 목록 조회하기",
            description = """
        """
    )
    @GetMapping("/list")
    public ResponseEntity<List<FriendListResponse>> getFriendList(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<FriendListResponse> friends = friendService.getFriendList(userDetails.getUsername());

        return ResponseEntity.ok(friends);
    }

}