package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.*;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse_Received;
import me.jsu.springbootdeveloper.dto.FriendRequestResponse_Sent;
import me.jsu.springbootdeveloper.repository.user.FriendRequestRepository;
import me.jsu.springbootdeveloper.repository.user.FriendshipRepository;
import me.jsu.springbootdeveloper.repository.user.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.repository.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserProfilePictureRepository userProfilePictureRepository;
    /**
     * 1. 친구 신청 보내기
     */
    @Transactional
    public void sendFriendRequest(String fromEmail, String toNickname) {
        User fromUser = userRepository.findByEmail(fromEmail).orElseThrow();
        User toUser = userRepository.findByNickname(toNickname)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        // 자기 자신에게 신청 방지
        if (fromUser.equals(toUser)) {
            throw new IllegalStateException("자기 자신에게는 친구 신청을 할 수 없습니다.");
        }

        // 중복 신청 방지 (existsRequested 활용)
        if (friendRequestRepository.existsRequested(fromUser, toUser)) {
            throw new IllegalStateException("이미 대기 중인 신청이 있습니다.");
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .build();

        friendRequestRepository.save(friendRequest);
    }
    /**
     * 나에게 온 친구 요청 목록 조회 (PENDING 상태)
     */
    @Transactional(readOnly = true)
    public List<FriendRequestResponse_Received> getReceivedRequests(String email) {
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        List<FriendRequest> requests = friendRequestRepository
                .findAllByToUser_IdAndStatus(currentUser.getId(), FriendRequestStatus.REQUESTED);

        return requests.stream()
                .map(request -> {
                    // 신청한 유저(fromUser)의 프로필 사진을 조회
                    String profileUrl = userProfilePictureRepository.findByUser_Id(request.getFromUser().getId())
                            .map(UserProfilePicture::getImageUrl)
                            .orElse(null); // 사진이 없을 경우 기본값 처리

                    return new FriendRequestResponse_Received(request, profileUrl);
                })
                .collect(Collectors.toList());
    }

    //내가 보낸 친구 요청 조회
    @Transactional(readOnly = true)
    public List<FriendRequestResponse_Sent> getSentRequests(String email) {
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        //Repository 메서드 활용
        List<FriendRequest> requests = friendRequestRepository
                .findAllByFromUser_IdAndStatus(currentUser.getId(), FriendRequestStatus.REQUESTED);

        return requests.stream()
                .map(request -> {
                    // 신청한 유저(fromUser)의 프로필 사진을 조회
                    String profileUrl = userProfilePictureRepository.findByUser_Id(request.getToUser().getId())
                            .map(UserProfilePicture::getImageUrl)
                            .orElse(null); // 사진이 없을 경우 기본값 처리

                    return new FriendRequestResponse_Sent(request, profileUrl);
                })
                .collect(Collectors.toList());
    }
    /**
     * 2. 친구 신청 수락
     */
    @Transactional
    public void acceptFriendRequest(Long requestId, String currentUserEmail) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();

        // 수신자 검증
        if (!request.getToUser().getEmail().equals(currentUserEmail)) {
            throw new IllegalStateException("본인에게 온 요청만 수락할 수 있습니다.");
        }

        // 상태 변경 및 Friendship 생성
        request.friendRequestAccepted();

        User user1 = (request.getFromUser().getId() < request.getToUser().getId())
                ? request.getFromUser() : request.getToUser();
        User user2 = (request.getFromUser().getId() < request.getToUser().getId())
                ? request.getToUser() : request.getFromUser();

        if (!friendshipRepository.existsByUser1AndUser2(user1, user2)) {
            friendshipRepository.save(Friendship.builder().user1(user1).user2(user2).build());
        }
        // 2. 친구가 되었으므로 기존의 친구 요청(FriendRequest)은 삭제
        // 삭제를 하면 '받은 요청'이나 '보낸 요청' 목록 조회 시 더 이상 나타나지 않습니다.
        friendRequestRepository.delete(request);

    }

    /**
     * 3. 친구 신청 거절 및 취소
     */
    @Transactional
    public void rejectOrCancelRequest(Long requestId, String currentUserEmail, boolean isReject) {
        FriendRequest request = friendRequestRepository.findById(requestId).orElseThrow();

        if (isReject) { // 거절 (받은 요청 탭)
            if (!request.getToUser().getEmail().equals(currentUserEmail)) throw new IllegalStateException("권한 없음");
            request.friendRequestRejected();
        } else { // 취소 (보낸 요청 탭)
            if (!request.getFromUser().getEmail().equals(currentUserEmail)) throw new IllegalStateException("권한 없음");
            request.friendRequestCanceled();
        }
    }
}