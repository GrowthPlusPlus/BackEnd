package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeInviteListResponse;
import me.jsu.springbootdeveloper.challenge.enumType.InviteStatus;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeInviteNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeInviteRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.repository.TagToChallengeRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeInviteService {
    private final ChallengeInviteRepository challengeInviteRepository;
    private final ChallengeRepository challengeRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;
    private final UserService userService;
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeService challengeService;
    private final TagToChallengeRepository tagToChallengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final UserProfilePictureRepository userProfilePictureRepository;
    /*
       작성된 함수 목록
       - 친구를 챌린지에 초대하는 함수/public void inviteUser(String inviterEmail, Long challengeId, String targetNickname)
       -
     */

    /*
        친구를 챌린지에 초대하는 함수이다.
        (1) 알림 전송 (2) 초대 테이블에 'INVITED' 상태로 저장
     */
    @Transactional
    public void inviteUser(String inviterEmail, Long challengeId, String targetNickname) {
        User inviter = userService.findByEmail(inviterEmail);
        Challenge challenge = challengeQueryService.findById(challengeId);
        User invitee = userService.findByNickname(targetNickname);


        //초대 정보 저장
        ChallengeInvite invite = ChallengeInvite.builder()
                .challenge(challenge)
                .inviter(inviter)
                .invitee(invitee)
                .build();

        challengeInviteRepository.save(invite);

        //초대 알림 전송
        //전체 알림 활성화 여부 검사 후 초대되는 사람에게 푸시알림 전송
        String message = inviter.getNickname() + "님이 " + challenge.getTitle()+ " 방에 나를 초대했어요.";
        String title = "챌린지 초대";
        notificationService.sendNotification(invitee, NotificationType.CHALLENGE, message); // 예시 메서드
        if (invitee.isAllPushNotificationEnabled()
                && invitee.getFcmToken() != null
                && !invitee.getFcmToken().isBlank()) {
            fcmService.sendMessageTo(invitee.getFcmToken(), title, message);
        }
    }
    /*
        알림 페이지에서 챌린지 초대 목록을 반환하는 함수이다.
        특정 유저가 받은 초대 목록 중 만료되지 않은 챌린지만 반환한다.
        반환값: 초대자 닉네임,유저 프로필 url, 챌린지 이름, 참여자 수, 완료까지 남은 날짜, 태그 목록
     */
    @Transactional
    public List<ChallengeInviteListResponse> getChallengeInviteList(String email) {
        // 이메일로 유저 정보 조회
        User user = userService.findByEmail(email);
        
        // 오늘 날짜를 기준으로 만료되지 않은 초대 목록 조회
        LocalDate today = LocalDate.now();
        List<ChallengeInvite> invites = challengeInviteRepository.findActiveInvitesByInvitee_Id(
                user.getId(),
                today
        );
        
        // 각 초대 정보를 DTO로 변환
        return invites.stream()
                .map(invite -> {
                    Challenge challenge = invite.getChallenge();
                    
                    // 초대자 닉네임
                    String inviterNickname = invite.getInviter().getNickname();

                    //초대자 프로필 url
                    Long inviterId = invite.getInviter().getId();
                    // findImageUrlByUserId가 값이 없으면 자동으로 ""를 넣어줍니다.
                    String imageUrl = userProfilePictureRepository.findImageUrlByUserId(inviterId).orElse("");

                    //챌린지 id
                    Long challengeId = challenge.getId();
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
                    
                    // 해당 챌린지의 태그 목록 조회
                    List<TagToChallenge> tagMappings = tagToChallengeRepository.findWithTagByChallengeId(
                            challenge.getId()
                    );
                    // 태그 이름만 추출하여 리스트로 변환
                    List<String> tags = tagMappings.stream()
                            .map(ttc -> ttc.getTag().getTagName())
                            .collect(Collectors.toList());
                    
                    // DTO 생성 및 반환
                    return new ChallengeInviteListResponse(
                            inviterNickname,
                            imageUrl,
                            challengeId,
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
        초대 상태를 ACCEPTED로 변경하고, 아직 참여하지 않은 경우 챌린지에 참여시킨다.
        초대 시점에 이미 참여 처리를 했을 수 있으므로, 중복 참여를 방지한다.
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
