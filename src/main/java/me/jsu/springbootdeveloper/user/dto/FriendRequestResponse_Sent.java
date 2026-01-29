package me.jsu.springbootdeveloper.user.dto;

import lombok.Getter;
import me.jsu.springbootdeveloper.user.domain.FriendRequest;

import java.time.LocalDateTime;

@Getter
//내가 보낸 친구 요청 dto
public class FriendRequestResponse_Sent {
    private final Long requestId;        // 수락/거절 처리를 위한 ID
    private final Long toUserId;      // 신청한 유저의 식별자
    private final String nickname;      // 화면에 표시할 닉네임
    private final String profileImageUrl; // 화면에 표시할 프로필 이미지 주소
    private final LocalDateTime createdAt; // 언제 신청했는지 (선택 사항)


    public FriendRequestResponse_Sent(FriendRequest friendRequest, String profileImageUrl) {
        this.requestId = friendRequest.getId();

        // '내가 보낸 요청' 목록이므로, 내가 보낸 사람(toUser)의 정보를 담습니다.
        this.toUserId = friendRequest.getToUser().getId();
        this.nickname = friendRequest.getToUser().getNickname();
        this.profileImageUrl = profileImageUrl;
        this.createdAt = friendRequest.getCreatedAt();
    }
}