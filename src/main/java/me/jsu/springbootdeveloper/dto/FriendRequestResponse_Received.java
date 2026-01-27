package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.domain.user.FriendRequest;

import java.time.LocalDateTime;

@Getter
//내가 받은 친구 요청 dto
public class FriendRequestResponse_Received {
    private final Long requestId;        // 수락/거절 처리를 위한 ID
    private final Long fromUserId;      // 신청한 유저의 식별자
    private final String nickname;      // 화면에 표시할 닉네임
    private final String profileImageUrl; // 화면에 표시할 프로필 이미지 주소
    private final LocalDateTime createdAt; // 언제 신청했는지 (선택 사항)


    public FriendRequestResponse_Received(FriendRequest friendRequest, String profileImageUrl) {
        this.requestId = friendRequest.getId();

        // '받은 요청' 목록이므로, 나에게 보낸 사람(fromUser)의 정보를 담습니다.
        this.fromUserId = friendRequest.getFromUser().getId();
        this.nickname = friendRequest.getFromUser().getNickname();
        this.profileImageUrl = profileImageUrl;
        this.createdAt = friendRequest.getCreatedAt();
    }
}