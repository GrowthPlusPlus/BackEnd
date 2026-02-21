package me.jsu.springbootdeveloper.user.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteRepository;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.user.dto.FriendListResponse;
import me.jsu.springbootdeveloper.user.dto.FriendRequestResponse_Received;
import me.jsu.springbootdeveloper.user.dto.FriendRequestResponse_Sent;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.user.repository.FriendRequestRepository;
import me.jsu.springbootdeveloper.user.repository.FriendshipRepository;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.domain.FriendRequest;
import me.jsu.springbootdeveloper.user.domain.Friendship;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.user.enumType.FriendRequestStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserProfilePictureRepository userProfilePictureRepository;
    private final ChallengeInviteRepository challengeInviteRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final FcmService fcmService;  // FCM 푸시 알림 발송을 위한 서비스
    private final UserService userService;

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
        //이미 친구인지 확인
        if (friendshipRepository.existsByUser1AndUser2(fromUser, toUser)){
            throw new IllegalStateException("이미 친구인 사용자입니다.");
        }
        //나->상대방 신청 보낸게 잇는지 확인
        FriendRequest existingRequest = friendRequestRepository.findByFromUserAndToUser(fromUser, toUser);
        //FriendRequest 테이블에 행이 존재한다면
        if (existingRequest != null) {
            // 아직 대기 중인 신청이면 중복 신청을 차단한다.
            if (existingRequest.getStatus().equals(FriendRequestStatus.REQUESTED)) {
                throw new IllegalStateException("이미 대기 중인 신청이 있습니다.");
            }
            // 취소 또는 거절 상태이면 기존 행을 삭제하고 재신청한다.
            friendRequestRepository.delete(existingRequest);
            friendRequestRepository.flush();  // DELETE를 즉시 DB에 반영하여 unique 제약 위반을 방지한다.
        }

        FriendRequest friendRequest = FriendRequest.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .build();

        friendRequestRepository.save(friendRequest);

        // 친구 요청을 받은 사용자(toUser)에게 푸시 알림을 발송한다.
        if (toUser.getFcmToken() != null && !toUser.getFcmToken().isBlank()) {
            String title = "친구 신청";
            String message = fromUser.getNickname() + "로부터 친구 신청이 왔습니다.";
            fcmService.sendMessageTo(toUser.getFcmToken(), title, message);
        }
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
                    // 신청한 유저(fromUser)의 프로필 사진을 조회한다. fetch로 이미 사진 객체를 가져왔다.
                    User fromUser = request.getFromUser();

                    UserProfilePicture picture = fromUser.getUserProfilePicture();
                    String profileUrl = (picture != null) ? picture.getImageUrl() : null;  // 사진이 없다면 링크를 null 처리한다.

                    return new FriendRequestResponse_Received(request, profileUrl);
                })
                .collect(Collectors.toList());
    }

    // 내가 보낸 친구 요청 조회
    @Transactional(readOnly = true)
    public List<FriendRequestResponse_Sent> getSentRequests(String email) {
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        // Repository 메서드 활용
        List<FriendRequest> requests = friendRequestRepository
                .findAllByFromUser_IdAndStatus(currentUser.getId(), FriendRequestStatus.REQUESTED);

        return requests.stream()
                .map(request -> {
                    // 신청한 유저(toUser)의 프로필 사진을 조회
                    User fromUser = request.getFromUser();

                    UserProfilePicture picture = fromUser.getUserProfilePicture();
                    String profileUrl = (picture != null) ? picture.getImageUrl() : null;  // 사진이 없다면 링크를 null 처리한다.

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

       //friendship 생성
        User user1 = (request.getFromUser().getId() < request.getToUser().getId())
                ? request.getFromUser() : request.getToUser();
        User user2 = (request.getFromUser().getId() < request.getToUser().getId())
                ? request.getToUser() : request.getFromUser();

        //이미 친구상태인지 체크
        if (!friendshipRepository.existsByUser1AndUser2(user1, user2)) {
            friendshipRepository.save(Friendship.builder().user1(user1).user2(user2).build());
        }
        // 2. 친구가 되었으므로 기존의 친구 요청(FriendRequest)은 삭제
        // 삭제를 하면 '받은 요청'이나 '보낸 요청' 목록 조회 시 더 이상 나타나지 않습니다.
        friendRequestRepository.delete(request);

        //내가 상대방한테 보낸 친구신청이 있으면 삭제
        if (friendRequestRepository.existsRequested(request.getToUser(), request.getFromUser())){
            FriendRequest friendRequest = friendRequestRepository.findByFromUserAndToUser(request.getToUser(), request.getFromUser());
            friendRequestRepository.delete(friendRequest);
        }

        // 친구 신청을 보낸 사용자(fromUser)에게 푸시 알림을 발송한다.
        // 푸시 알림이 활성화되어 있고, FCM 토큰이 있는 경우에만 발송한다.
        User fromUser = request.getFromUser();
        if (fromUser.isFriendPushNotificationEnabled() 
                && fromUser.getFcmToken() != null 
                && !fromUser.getFcmToken().isBlank()) {
            String title = "친구 신청 수락";
            String message = request.getToUser().getNickname() + "님이 친구 신청을 수락하였습니다.";
            fcmService.sendMessageTo(fromUser.getFcmToken(), title, message);
        }
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

    /**
     * 친구 삭제
     */
    @Transactional
    public void deleteFriend(String currentUserEmail, String friendNickname) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User friend = userRepository.findByNickname(friendNickname)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 친구를 찾을 수 없습니다."));

        friendshipRepository.deleteFriendship(user, friend);
    }

    /**
     * 친구 목록 조회
     */
    @Transactional(readOnly = true)
    public List<FriendListResponse> getFriendList(String email) {
        User user = userService.findByEmail(email);

        List<User> friends = friendshipRepository.findAllFriendsByUserId(user.getId());

        return friends.stream()
                .map(friend -> {
                    // 친구의 사진을 조회한다.
                    UserProfilePicture picture = friend.getUserProfilePicture();
                    String profileUrl = (picture != null) ? picture.getImageUrl() : null;

                    return new FriendListResponse(friend, profileUrl);
                })
                .collect(Collectors.toList());
    }
}