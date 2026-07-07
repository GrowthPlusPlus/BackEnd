package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import me.jsu.springbootdeveloper.challenge.dto.invite.ChallengeInviteListResponse;
import me.jsu.springbootdeveloper.challenge.enumType.InviteStatus;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotInProgressException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.tag.repository.TagToChallengeRepository;
import me.jsu.springbootdeveloper.tag.service.TagService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeInviteService {
    private final ChallengeInviteRepository challengeInviteRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final UserService userService;
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeService challengeService;
    private final TagToChallengeRepository tagToChallengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final TagService tagService;

    /*
       작성된 함수 목록
       - 친구를 챌린지에 초대하는 함수/public void inviteUser(String inviterEmail, Long challengeId, String targetNickname)
       -
     */

    /*
        친구를 챌린지에 초대하는 함수이다.
        (1) 초대 가능 여부 검증 (2) 초대 테이블에 'INVITED' 상태로 저장 (3) 알림 전송
        실제 챌린지 참여는 invitee가 수락(acceptInvite)할 때 처리한다.

        @param inviterEmail 초대를 보내는 유저 이메일
        @param challengeId 초대할 챌린지 ID
        @param targetNickname 초대 대상 친구 닉네임
     */
    @Transactional
    public void inviteUser(String inviterEmail, Long challengeId, String targetNickname) {
        User inviter = userService.findByEmail(inviterEmail);
        Challenge challenge = challengeQueryService.findById(challengeId);
        User invitee = userService.findByNickname(targetNickname);
        LocalDate today = LocalDate.now();

        // 진행 중인 챌린지인지 확인한다.
        if (challenge.getEndDate().isBefore(today)) {
            throw new ChallengeNotInProgressException("진행중인 챌린지가 아닙니다.");
        }

        // 이미 참여 중인 유저는 초대할 수 없다.
        if (challengeParticipantRepository.existsByUser_IdAndChallenge_Id(
                invitee.getId(),
                challengeId
        )) {
            throw new IllegalStateException("이미 참여 중인 챌린지입니다.");
        }

        // 정원이 가득 찬 챌린지에는 초대할 수 없다.
        if (challenge.checkIfOverMaxParticipantNumber()) {
            throw new IllegalStateException("챌린지 참여 정원이 꽉 찼습니다");
        }

        // INVITED 상태의 중복 초대를 방지한다. (거절 후 재초대는 허용)
        challengeInviteRepository.findByChallenge_IdAndInvitee_Id(challengeId, invitee.getId())
                .ifPresent(existingInvite -> {
                    if (existingInvite.getStatus() == InviteStatus.INVITED) {
                        throw new NotAllowedChallengeActionException("이미 초대된 유저입니다.");
                    }
                });

        // 초대 정보 저장 (ChallengeInvite 생성자 기본 상태: INVITED)
        ChallengeInvite invite = ChallengeInvite.builder()
                .challenge(challenge)
                .inviter(inviter)
                .invitee(invitee)
                .build();

        challengeInviteRepository.save(invite);

        // 초대 알림 전송
        // 전체 알림이 아니라 "챌린지 초대 푸시 알림" 활성화 여부만 확인 후 초대되는 사람에게 푸시알림 전송
        String message = inviter.getNickname() + "님이 " + challenge.getTitle()+ " 방에 나를 초대했어요.";
        String title = "챌린지 초대";
        notificationService.sendNotification(invitee, NotificationType.CHALLENGE, message); // 예시 메서드
        if (invitee.isChallengeInvitePushNotificationEnabled()
                && invitee.getFcmToken() != null
                && !invitee.getFcmToken().isBlank()) {
            fcmService.sendMessageTo(invitee.getFcmToken(), title, message);
        }
    }
    /*
        알림 페이지에서 챌린지 초대 목록을 반환하는 함수이다.
        특정 유저가 받은 초대 목록 중 만료되지 않은 챌린지만 반환한다.
        반환값: 초대자 닉네임, 챌린지 이름, 참여자 수, 완료까지 남은 날짜, 태그 목록
     */
    @Transactional(readOnly = true)
    public List<ChallengeInviteListResponse> getChallengeInviteList(String email) {
        // 이메일로 유저 정보 조회
        User user = userService.findByEmail(email);
        
        // 오늘 날짜를 기준으로 만료되지 않은 초대 목록 조회
        LocalDate today = LocalDate.now();
        List<ChallengeInvite> invites = challengeInviteRepository.findActiveInvitesByInvitee_Id(
                user.getId(),
                today
        );

        // challengeId 목록
        List<Long> challengeIds = invites.stream()
                .map(invite -> invite.getChallenge().getId())
                .distinct()
                .toList();

        // 태그 한 번에 조회
        Map<Long, List<TagResponse>> tagMap = tagService.getChallengeTagsByOnce(challengeIds);
        
        // 각 초대 정보를 DTO로 변환
        return invites.stream()
                .map(invite -> {
                    Challenge challenge = invite.getChallenge();
                    
                    // 초대자 닉네임
                    String inviterNickname = invite.getInviter().getNickname();
                    
                    // 챌린지 이름
                    String challengeTitle = challenge.getTitle();
                    
                    // 현재 참여자 수 (Challenge 엔티티의 participantNumber 필드 사용)
                    int participantCount = challenge.getParticipantNumber();
                    
                    // 완료까지 남은 날짜 계산 (endDate - today)
                    long remainingDays = ChronoUnit.DAYS.between(today, challenge.getEndDate());
                    // 오늘이 종료일인 경우 0일로 표시
                    if (remainingDays < 0) {
                        remainingDays = 0;
                    }

                    // 미리 생성한 챌린지 id와 태그 목록으로 챌린지에 맞는 태그를 결합한다.
                    List<TagResponse> tags = tagMap.getOrDefault(
                            challenge.getId(),
                            List.of()
                    );

                    // 초대자 프로필 이미지 URL (없으면 null)
                    String profileUrl = null;
                    if (invite.getInviter() != null && invite.getInviter().getUserProfilePicture() != null) {
                        profileUrl = invite.getInviter().getUserProfilePicture().getImageUrl();
                    }
                    
                    // DTO 생성 및 반환
                    return new ChallengeInviteListResponse(
                            invite.getInviter().getId(),
                            inviterNickname,
                            profileUrl,
                            challenge.getId(),
                            challengeTitle,
                            participantCount,
                            remainingDays,
                            tags
                    );
                })
                .collect(Collectors.toList());
    }

    /*
        챌린지 초대를 수락하는 함수이다.
        초대 상태를 ACCEPTED로 변경하고, 챌린지에 참여시킨다.
     */
    @Transactional
    public void acceptInvite(String email, Long challengeId) {
        // 이메일로 유저 정보 조회
        User user = userService.findByEmail(email);
        
        // 초대 정보 조회 (챌린지 id와 초대받은 사람 id로)
        ChallengeInvite invite = challengeInviteRepository.findByChallenge_IdAndInvitee_Id(
                challengeId,
                user.getId()
        ).orElseThrow(() -> new ChallengeInviteNotFoundException("CHALLENGE_INVITE_NOT_FOUND"));
        
        // 초대 상태가 INVITED인지 확인 (이미 수락하거나 거절한 경우 처리 불가)
        if (invite.getStatus() != InviteStatus.INVITED) {
            throw new NotAllowedChallengeActionException("이미 처리된 초대입니다.");
        }
        
        Challenge challenge = invite.getChallenge();
        

        // 이미 참여 중인지 확인 (중복 참여 방지)
        boolean isAlreadyParticipant = challengeParticipantRepository.findByUser_IdAndChallenge_Id(
                user.getId(),
                challengeId
        ).isPresent();
        
        
        // 아직 참여하지 않은 경우에만 챌린지에 참여
        if (!isAlreadyParticipant) {
            // 챌린지 참여 인원수 확인 후 참여
            if (!challengeService.joinChallenge(challenge, user)) {
                // 참여 못했다면 예외 발생
                throw new IllegalStateException("챌린지에 참여할 수 없습니다.");
            }
        }
        else{
            throw new NotAllowedChallengeActionException("이미 참여한 챌린지입니다.");
        }
        
        // 초대 상태를 ACCEPTED로 변경
        invite.updateStatus(InviteStatus.ACCEPTED);
    }
    
    /*
        챌린지 초대를 거절하는 함수이다.
        초대 상태를 REJECTED로 변경한다.
     */
    @Transactional
    public void rejectInvite(String email, Long challengeId) {
        // 이메일로 유저 정보 조회
        User user = userService.findByEmail(email);
        
        // 초대 정보 조회 (챌린지 id와 초대받은 사람 id로)
        ChallengeInvite invite = challengeInviteRepository.findByChallenge_IdAndInvitee_Id(
                challengeId,
                user.getId()
        ).orElseThrow(() -> new ChallengeInviteNotFoundException("CHALLENGE_INVITE_NOT_FOUND"));
        
        // 초대 상태가 INVITED인지 확인 (이미 수락하거나 거절한 경우 처리 불가)
        if (invite.getStatus() != InviteStatus.INVITED) {
            throw new NotAllowedChallengeActionException("이미 처리된 초대입니다.");
        }
        
        // 초대 상태를 REJECTED로 변경
        invite.updateStatus(InviteStatus.REJECTED);
    }
    
}
