// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import me.jsu.springbootdeveloper.challenge.dto.ChallengeSettingsResponse;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeFriendInviteListResponse;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import me.jsu.springbootdeveloper.challenge.repository.*;
import me.jsu.springbootdeveloper.challenge.dto.SearchChallengeResponse;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.exception.UserStatisticsNotFoundException;
import me.jsu.springbootdeveloper.user.repository.FriendshipRepository;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotInProgressException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeOwnerCannotLeaveException;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeMemberResponse;
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.tag.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import me.jsu.springbootdeveloper.user.dto.FriendListResponse;
import me.jsu.springbootdeveloper.challenge.dto.CreateChallengeRequest;
import me.jsu.springbootdeveloper.challenge.dto.CreateChallengeResponse;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.service.FriendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.stream.Collectors;

/*
    챌린지와 관련된 일을 처리하는 클래스이다.
 */
@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeDailyLogRepository challengeDailyLogRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;
    private final ChallengeInviteLinkRepository challengeInviteLinkRepository;
    private final TagService tagService;
    private final UserService userService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    //base url(application.yml의 base url값 불러옴)
    @Value("${app.base-url}")
    private String baseUrl;
    //챌린지 생성 시 친구 목록 가져와서 전달해야 함
    private final FriendService friendService;
    private final ChallengeInviteRepository challengeInviteRepository;
    private final NotificationService notificationService;
    private final UserProfilePictureRepository userProfilePictureRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserStatisticsRepository userStatisticsRepository;  // 순환 참조를 방지하기 위해 직접 사용한다.

    //챌린지 생성 기능
    @Transactional
    public CreateChallengeResponse createChallenge(String email, CreateChallengeRequest dto) {
        //로그인한 유저 정보 조회 (인증 객체에서 넘어온 email 사용)
        User user = userService.findByEmail(email);

        //챌린지 저장하기
        // 종료일 계산 로직 (시작일 + 인증 기간), 화면에서 '30일'을 선택했다면 startDate로부터 30일 뒤를 계산합니다.
        LocalDate endDate = dto.getStartDate().plusDays(dto.getDuration());

        //Challenge 객체 생성 (Builder 패턴 사용)
        Challenge challenge = Challenge.builder()
                .title(dto.getTitle())
                .content(dto.getDescription())
                //챌린지 생성자(방장)
                .user(user)
                // 화면에 필드가 없으므로 디폴트 50
                .maxParticipantNumber((dto.getMaxParticipantNumber() != null) ? dto.getMaxParticipantNumber() : 50)
                // 화면에 필드가 없으므로 디폴트 PUBLIC
                .challengeVisibility((dto.getChallengeVisibility() != null) ? dto.getChallengeVisibility() : ChallengeVisibility.PUBLIC)
                .startDate(dto.getStartDate())
                .endDate(endDate)
                .requiredWeeklyCount(dto.getFrequency())
                .photoRequired(dto.isPhotoRequired())
                .build();

        //챌린지 객체 db에 저장
        Challenge savedChallenge = challengeRepository.save(challenge);

        // 생성자는 해당 챌린지에 참여
        boolean joinchallenge = joinChallenge(savedChallenge, user);

        //공유 Url 생성
        String inviteUrl = baseUrl + "/challenges/" + savedChallenge.getId();

        //친구 목록 가져오기
        List<FriendListResponse> friends = friendService.getFriendList(email);

        //(1) url 링크, (2)친구 목록 반환하기
        return new CreateChallengeResponse(savedChallenge,inviteUrl, friends);

    }
    /*
        친구를 챌린지에 초대하는 함수이다.
        (1) 알림 전송 (2) 초대 테이블에 'INVITED' 상태로 저장
     */
    @Transactional
    public void inviteUser(String inviterEmail, Long challengeId, String targetNickname) {
        User inviter = userService.findByEmail(inviterEmail);
        Challenge challenge = findById(challengeId);
        User invitee = userService.findByNickname(targetNickname);
        //챌린지 참여 인원수 확인 후 참여
        if (!joinChallenge(challenge, invitee))
        {
            //참여 못했다면 exception 발동
            throw new IllegalStateException("챌린지 참여 정원이 꽉 찼습니다");

        }
        //초대 알림 전송 (NotificationService 구현에 따라 호출)
        String message = inviter.getNickname() + "님이 " + challenge.getTitle()+ " 방에 나를 초대했어요.";
        notificationService.sendNotification(invitee, NotificationType.CHALLENGE, message); // 예시 메서드

        //초대 정보 저장
        ChallengeInvite invite = ChallengeInvite.builder()
                .challenge(challenge)
                .inviter(inviter)
                .invitee(invitee)
                .build();

        challengeInviteRepository.save(invite);

    }
    // 챌린지 참여하기
    // 반환 값: 참여 성공하면 true, 실패하면 false
    public boolean joinChallenge(Challenge challenge, User user) {
        // 해당 챌린지 인원수 체크
        // 내가 참여했을 때 challenge 의 maxParticipant를 초과하면 들어가지 못함
        if (!challenge.checkIfOverMaxParticipantNumber()){
            LocalDate today = LocalDate.now();
            /* joinDate 설정
             case 1. 참여하는 사람이 챌린지 시작일 전에 들어오면 -> joinDate는 챌린지 시작일
             case 2. 참여하는 사람이 챌린지 시작일 후에 들어오면 -> joinDate는 들어온 날짜
            * */
            LocalDate joinDate = today.isBefore(challenge.getStartDate()) ? challenge.getStartDate() : today;
            // totalWeeks 값:  6일 챌린지 -> 0, 7일 챌린지 -> 1,  8일 챌린지 -> 1
            int totalWeeks = (int) ChronoUnit.WEEKS.between(joinDate, challenge.getEndDate());
            // 챌린지 참여자 객체 생성
            ChallengeParticipant challengeParticipant = new ChallengeParticipant(challenge, user, joinDate, totalWeeks);
            challengeParticipantRepository.save(challengeParticipant);
            // 챌린지 참여자 수 업데이트
            challenge.updateParticipate();

            // 유저 통계의 참여 챌린지 개수를 업데이트 한다. 없다면 생성한다.
            UserStatistics userStatistics = userStatisticsRepository.findByUser_Id(user.getId())
                    .orElseGet(() -> userStatisticsRepository.save(UserStatistics.builder()
                            .user(user)
                            .build()));
            userStatistics.increaseChallengeCount();

            return true;
        }
        else
            return false;
    }

    /**
     * 친구 목록과 챌린지 초대 상태 조회
     * 이미 참여 중인 유저는 제외하고, 초대됨/초대 가능 상태인 유저만 반환한다.
     */
    @Transactional(readOnly = true)
    public List<ChallengeFriendInviteListResponse> getFriendList_with_InviteStatus(String email, Long challengeId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //전체 친구 목록 조회
        List<User> friends = friendshipRepository.findAllFriendsByUserId(user.getId());

        return friends.stream()
                //이미 참여 중인 친구는 제외
                .filter(friend-> !challengeParticipantRepository.existsByChallenge_IdAndUser_Id(challengeId, friend.getId()))
                .map(friend -> {
                    // 친구의 프로필 사진 조회
                    String profileUrl = userProfilePictureRepository.findByUser_Id(friend.getId())
                            .map(UserProfilePicture::getImageUrl)
                            .orElse(null); // 사진이 없을 경우 기본값 처리

                    //해당 친구의 초대 상태 판별(1. 초대되거나 2.초대되지 않음 (해당 초대 테이블에 저장된 정보 없음))
                    boolean inviteStatus = challengeInviteRepository.existsByChallenge_IdAndInvitee_Id(challengeId, friend.getId());
                    return new ChallengeFriendInviteListResponse(friend, profileUrl, inviteStatus);
                })
                .collect(Collectors.toList());
    }

    /**
     * 키워드로 챌린지를 검색한다. (제목 또는 태그명 포함)
     */
    public Page<SearchChallengeResponse> searchChallenges(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 20);

        // 검색어가 포함된 공개 챌린지를 조회한다.
        Page<Challenge> challenges = challengeRepository.searchChallenges(
                keyword, ChallengeVisibility.PUBLIC, pageable);

        // 2. DTO 변환 (생성자에서 필요한 모든 필드 매핑)
        return challenges.map(challenge -> {
            // 태그 목록은 성능을 위해 별도로 fetch join하거나
            // application.yml의 default_batch_fetch_size 설정을 권장합니다.
            List<TagResponse> tags = tagService.getChallengeTags(challenge.getId());

            return new SearchChallengeResponse(challenge, tags);
        });

    }

    /*
        챌린지 id로 챌린지를 검색해주는 함수이다.
     */
    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_NOT_FOUND_FOR_ID"));
    }

    /*
        유저 id로 유저가 참여한 챌린지 중, 현재 진행중인 챌린지를 반환하는 함수이다.
     */
    public List<ChallengeParticipant> findByUser_IdForCpInProgress(Long userId) {
        return challengeParticipantRepository.findByUser_IdForCpInProgress(userId);
    }

    /*
        유저 id로 유저가 참여한 챌린지 중, 현재 진행중인 챌린지를 반환해주는 함수이다. 챌린지 정보도 포함하여 리턴한다.
     */
    public List<ChallengeParticipant> findInProgressWithChallengeByUserId(Long userId) {
        return challengeParticipantRepository.findInProgressWithChallengeByUserId(userId);
    }

    /*
        유저 id와 챌린지 상태로 참여한 챌린지를 최신순으로 반환해주는 함수이다.
     */
    public List<ChallengeParticipant> findStatusWithChallengeByUserIdAndStatus(Long userId, Status status) {
        return challengeParticipantRepository.findStatusWithChallengeByUserIdAndStatus(userId, status);
    }

    /*
        유저 id와 날짜로 해당 날짜에 수행한 데일리 로그를 반환해주는 함수이다.
     */
    public List<ChallengeDailyLog> findByUser_IdAndPostDate(Long userId, LocalDate targetDate) {
        return challengeDailyLogRepository.findByUser_IdAndPostDate(userId, targetDate);
    }

    /*
        유저 id와 이번주 시작일로 해당 주에 유저가 챌린지를 몇 회 수행했는지 등에 관한 정보를 반환해주는 함수이다.
     */
    public List<ChallengeWeeklyStatus> findByUser_IdAndWeekStartDate(Long userId, LocalDate weekStartDate) {
        return challengeWeeklyStatusRepository.findByUser_IdAndWeekStartDate(userId, weekStartDate);
    }

    /*
        유저가 관리하는 모든 챌린지를 검색한다.
     */
    public List<Challenge> findByUser_Id(Long userId) {
        return challengeRepository.findByUser_Id(userId);
    }

    /*
        유저가 회원탈퇴를 하는 경우, 해당 유저가 오너인 챌린지가 삭제되지 않도록 다른 유저에게 위임한다.
     */
    @Transactional
    public void handleOwnerWithdrawal(Long userId) {
        List<Challenge> challenges = findByUser_Id(userId);  // 현재 관리하는 챌린지들을 검색한다.

        for (Challenge challenge : challenges) {
            List<ChallengeParticipant> participants =  // 오너를 제외한 챌린지 참가자를 검색한다.
                    challengeParticipantRepository.findParticipantsExceptOwner(challenge.getId(), userId);

            /*
                참여자가 오너 말고 없다면 챌린지를 삭제한다.
                단 챌린지를 삭제하는 행위는 또한 챌린지 CASCADE 설정이나 예외처리를 해야한다.
             */
            if (participants.isEmpty()) {
                deleteChallengeInternal(challenge.getId());
                continue;  // 다음 챌린지를 검색한다.
            }

            // 가장 먼저 참여한 사람에게 권한을 위임한다.
            User newMaster = participants.get(0).getUser();
            challenge.updateUser(newMaster);
        }
    }

    /*
        모든 챌린지에서 유저가 나올때 사용하는 함수이다. 회원탈퇴 시에 사용된다.
        해당 함수는 방장 위임이 끝난 뒤에 사용해야 한다.
     */
    @Transactional
    public void deleteParticipant(Long userId) {
        // 유저 아이디로 해당 유저가 참가한 챌린지들을 검색한다.
        List<ChallengeParticipant> participants = challengeParticipantRepository.findWithChallengeByUserId(userId);
        if (participants.isEmpty()) {  // 참여한 챌린지가 없다면 리턴한다.
            return;
        }

        for (ChallengeParticipant challengeParticipant : participants) {
            challengeParticipant.getChallenge().updateLeave();  // 챌린지 참여자 수를 감소시킨다.
            challengeParticipantRepository.delete(challengeParticipant);  // 챌린지 참여에서 삭제한다.
        }
    }

    /*
        인증글 id로 챌린지를 검색한다.
     */

    /*
        챌린지 데일리 로그의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    public void detachLogToUser(User user) {
        challengeDailyLogRepository.detachUser(user);
    }

    /*
        챌린지 데일리 로그를 유저 id, 인증글 id, 날짜로 검색한다.
     */
    public ChallengeDailyLog findLogByUser_IdAndArticle_IdAndPostDate(Long userId, Long articleId, LocalDate postDate) {
        return challengeDailyLogRepository.findByUser_IdAndArticle_IdAndPostDate(userId, articleId, postDate)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_LOG_NOT_FOUND"));
    }

    /*
        챌린지 데일리 로그의 인증글 fk값을 null로 바꿔주는 함수이다.
     */
    public void detachLogToArticle(Article article) {
        challengeDailyLogRepository.detachArticle(article);
    }

    /*
        챌린지 데일리 로그를 실제로 삭제한다.
     */
    public void deleteLog(ChallengeDailyLog challengeDailyLog) {
        challengeDailyLogRepository.delete(challengeDailyLog);
    }

    /*
        특정 챌린지를 완수한 경우, 연속 챌린지 달성 일수와 총 챌린지 일수를 업데이트하기 위해 호출하는 함수이다.
        반드시 하루에 최대 한 번만 수행한다.
     */
    public void succeedChallenge(Long userId, Long challengeId, LocalDate postDate) {
        // 챌린지 참여 DB를 검색한다.
        ChallengeParticipant challengeParticipant = findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);
        challengeParticipant.recordDailySuccess(postDate);  // 챌린지 참여 DB를 업데이트 한다.
    }

    /*
        챌린지 참여자 DB를 챌린지 id, 유저 id로 검색한다.
     */
    public ChallengeParticipant findByUser_IdAndChallenge_IdForParticipant(Long userId, Long challengeId) {
        return challengeParticipantRepository.findByUser_IdAndChallenge_Id(userId, challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_PARTICIPANT_NOT_FOUND"));
    }

    /*
        해당 챌린지에 대해 유저가 방장인지 검사한다.
     */
    public boolean isOwner(Long userId, Long challengeId) {
        return challengeRepository.existsByIdAndUser_Id(challengeId, userId);
    }

    /*
        isOwner에서 검사한 값을 바탕으로 해당 사용자가 방장인지 검사한다.
     */
    public void validateOwner(boolean challengeOwner) {
        if (!challengeOwner) {
            throw new NotAllowedChallengeActionException("NOT_CHALLENGE_OWNER");
        }
    }

    /*
        컨트롤러와 연결된 챌린지에서 유저가 나갈 수 있도록 하는 함수이다.
        컨트롤러-서비스 구조의 일관성을 위해 실제 처리 함수랑 분리한다.
     */
    @Transactional
    public void leaveChallenge(String email, Long challengeId) {
        User user = userService.findByEmail(email);
        leaveChallengeInternal(user.getId(), challengeId);
    }

    /*
        챌린지에서 유저가 나갈 수 있도록 하는 함수이다.
        방장은 나가기가 불가능하며, 관련된 종속 데이터를 정리한다.
        서비스 내부에서 사용하기 위해 유저 id로 삭제한다.
     */
    public void leaveChallengeInternal(Long userId, Long challengeId) {
        Challenge challenge = findById(challengeId);

        // 해당 챌린자 참여자를 검색한다.
        ChallengeParticipant challengeParticipant = findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);

        // 방장 여부를 판단한다.
        boolean ChallengeOwner = isOwner(userId, challengeId);

        // 방장은 나가기가 불가하다는 예외를 던진다.
        if (ChallengeOwner) {
            throw new ChallengeOwnerCannotLeaveException("CHALLENGE_OWNER_CANNOT_LEAVE");
        }

        /*
            챌린지에서 나가면 의미 없는 종속 DB 값들을 유저 id와 챌린지 id로 삭제한다.
            챌린지 초대 링크, 챌린지 위크 스테이터스 DB가 이에 해당한다.
         */
        challengeInviteLinkRepository.deleteByUser_IdAndChallenge_Id(userId, challengeId);
        challengeWeeklyStatusRepository.deleteByUser_IdAndChallenge_Id(userId, challengeId);

        // 참여자 정보를 실제로 삭제한다.
        challengeParticipantRepository.delete(challengeParticipant);

        // 챌린지의 전체 참여자 수를 감소시킨다.
        challenge.updateLeave();

        // 유저가 참여한 총 챌린지 개수를 감소시킨다.
        UserStatistics userStatistics = userStatisticsRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserStatisticsNotFoundException("통계를 찾을 수 없음"));
        userStatistics.decreaseChallengeCount();
    }

    /*
        챌린지에 참여한 유저를 검색하는 함수이다.
     */
    public Page<ChallengeMemberResponse> getMembers(String email, Long challengeId, int page, String nickname) {
        User user = userService.findByEmail(email);

        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 인증글 개수
                Sort.by(Sort.Order.asc("user.nickname"))  // 닉네임을 기준으로 오름차순 검색한다.
        );

        // 방장이 챌린지 참여자인지 확인한다.
        findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);

        // % 처리를 리포지토리가 아니라 서비스에서 한다.
        String keyword = (nickname == null || nickname.isBlank()) ? null : "%" + nickname + "%";
        Page<ChallengeParticipant> challengeParticipants = challengeParticipantRepository.findMembersWithUserAndPicture(challengeId, keyword, pageable);

        // 검색한 맴버를 ChallengeMemberResponse에 매핑하여 반환한다.
        return challengeParticipants.map(challengeParticipant -> {
            User userEntity = challengeParticipant.getUser();
            UserProfilePicture picture = userEntity.getUserProfilePicture();

            return new ChallengeMemberResponse(
                    userEntity.getId(),
                    userEntity.getNickname(),
                    picture != null ? picture.getImageUrl() : null  // 프로필 사진이 null값인지 아닌지 검사한다.
            );
        });
    }

    /*
        챌린지의 방장을 위임하기 위한 함수이다.
     */
    @Transactional
    public void delegateOwner(String email, Long challengeId, Long targetUserId) {
        User user = userService.findByEmail(email);

        // 챌린지를 조회한다.
        Challenge challenge = findById(challengeId);

        // 방장 권한을 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 위임 대상자가 참여자가 맞는지 검증하고 검색한다.
        ChallengeParticipant targetParticipant = findByUser_IdAndChallenge_IdForParticipant(targetUserId, challengeId);

        // 방장을 변경한다.
        challenge.updateUser(targetParticipant.getUser());

        // 챌린지에서 나간다.
        leaveChallengeInternal(user.getId(), challengeId);
    }

    /*
        챌린지에서 유저를 추방하기 위한 함수이다.
     */
    @Transactional
    public void kickMember(String email, Long challengeId, Long targetUserId) {
        User user = userService.findByEmail(email);

        // 추방하려는 사용자가 방장인지 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 자기 자신을 추방하는 것을 방지한다.
        if (user.getId().equals(targetUserId)) {
            throw new NotAllowedChallengeActionException("CANNOT_KICK_OWNER");
        }

        // 대상 유저를 추방한다.
        leaveChallengeInternal(targetUserId, challengeId);
    }

    /*
        챌린지를 삭제하는 서비스 클래스이다.
        컨트롤러-서비스 구조를 유지하기 위해 사용한다.
     */
    @Transactional
    public void deleteChallenge(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        // 방장 권한을 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 챌린지를 삭제한다.
        deleteChallengeInternal(challengeId);
    }

    /*
        챌린지를 실제로 삭제하는 서비스 클래스이다. 내부적으로 재사용하기 위해 분리하여 사용한다.
        데일리 로그, 인증글은 FK만 null 처리하고, 나머지 종속된 DB값들은 cascade 옵션으로 자동 삭제한다.
     */
    public void deleteChallengeInternal(Long challengeId) {
        Challenge challenge = findById(challengeId);

        // 데일리 로그, 인증글의 fK값을 null 처리한다.
        challengeDailyLogRepository.detachChallenge(challenge);
        postRepository.detachChallenge(challenge);  // 순환참조 방지를 위해 서비스를 사용하지 않고 직접 사용한다.

        // 챌린지를 삭제한다. 나머지 종속된 값들은 cascade REMOVE 옵션으로 자동 삭제된다.
        challengeRepository.delete(challenge);
    }

    /*
        사용자에게 기존 챌린지 설정 정보를 반환하는 함수이다.
     */
    public ChallengeSettingsResponse getSettings(Long challengeId) {
        Challenge challenge = findById(challengeId);

        // 챌린지 인증 기간을 int로 구한다.
        int endDate = (int) ChronoUnit.DAYS.between(challenge.getStartDate(), challenge.getEndDate());

        // 챌린지 id로 연관된 태그들을 검색한다.
        List<TagResponse> tagChallenge = tagService.getChallengeTags(challengeId);

        // 서버의 모든 태그를 검색한다.
        List<TagResponse> tags = tagService.getAllTags();

        return ChallengeSettingsResponse.builder()
                .title(challenge.getTitle())
                .startDate(challenge.getStartDate())
                .endDate(endDate)
                .requiredWeeklyCount(challenge.getRequiredWeeklyCount())
                .challengeTags(tagChallenge)
                .allTags(tags)
                .content(challenge.getContent())
                .photoRequired(challenge.isPhotoRequired())
                .challengeVisibility(challenge.getChallengeVisibility())
                .build();
    }

    /*
        챌린지 설정을 바꾸는 함수이다.
     */
    @Transactional
    public void updateSettings(String email, Long challengeId, int endDateInteger, int requiredWeeklyCount, List<Long> tagIds,
                String content, boolean photoRequired, ChallengeVisibility challengeVisibility) {
        User user = userService.findByEmail(email);
        Challenge challenge = findById(challengeId);

        // 방장인지 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // int로 들어온 종료일을 LocalDate 형식으로 변환한다.
        LocalDate endDate = challenge.getStartDate().plusDays(endDateInteger);

        tagService.deleteAllChallenge_Id(challengeId);  // 기존 챌린지-태그 매핑을 모두 삭제한다.

        // 요청 태그로 업데이트 한다.
        tagService.updateChallengeTags(tagIds, challenge);

        // 설정 값을 업데이트 한다. content값은 null이 될 수 있다.
        challenge.updateSettings(endDate, requiredWeeklyCount, content, photoRequired, challengeVisibility);
    }

    /*
        인증 글이 생성되었을 때, 데일리 로그를 생성 한다.
     */
    public void createDailyLog(User user, Challenge challenge, Article article, LocalDate postDate) {
        challengeDailyLogRepository.save(ChallengeDailyLog.builder()
                .user(user)
                .challenge(challenge)
                .article(article)
                .postDate(postDate)
                .build());
    }

    /*
        유저 id, 챌린지 id, 이번주 날짜로 챌린지 주간 상태 값을 리턴하는 함수이다.
     */
    public ChallengeWeeklyStatus findWeeklyStatus(Long userId, Long challengeId, LocalDate weekStartDate) {
        return challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                userId, challengeId, weekStartDate)
                .orElse(null);
    }

    /*
        인증글이 생성되었을 때, 챌린지 주간 상태를 업데이트 한다.
        월요일을 기준으로 이번주에 처음 인증을 하면 그 주에 해당하는 챌린지 주간 상태값이 생성된다.
        만약 주의 첫 인증이 아니라면 값을 업데이트 한다.
     */
    public void updateWeeklyStatus(User user, Challenge challenge, LocalDate weekStartDate, int requiredWeeklyCount) {
        // 이번주에 해당하는 챌린지 주간 상태 튜플이 있는지 검색한다.
       ChallengeWeeklyStatus challengeWeeklyStatus = findWeeklyStatus(user.getId(), challenge.getId(), weekStartDate);

        if (challengeWeeklyStatus == null) {  // 이번 주 첫 인증이면 새로 생성한다.
            challengeWeeklyStatus = ChallengeWeeklyStatus.builder()
                    .user(user)
                    .challenge(challenge)
                    .weekStartDate(weekStartDate)
                    .build();
            challengeWeeklyStatusRepository.save(challengeWeeklyStatus);
        }

        // 이번주 인증 횟수를 업데이트 한다.
        challengeWeeklyStatus.updateSuccessCount(requiredWeeklyCount);

        // 챌린지 참여 DB를 검색한다.
        ChallengeParticipant challengeParticipant = findByUser_IdAndChallenge_IdForParticipant(user.getId(), challenge.getId());

        // 이번 주차가 챌린지 시작 이후 몇 주차인지 검색한다.
        int weekIndex = calculateWeekIndex(challengeParticipant.getJoinDate(), weekStartDate);

        // 이번 주의 상태가 성공이라면 총 성공 주차 수를 업데이트 한다. 주에 한 번만 수행된다.
        if (challengeWeeklyStatus.getStatus() == Status.SUCCESS && challengeParticipant.getSuccessWeeks() < weekIndex) {
            challengeParticipant.increaseSuccessWeeks();
        }
    }

    /*
        인증글이 생성되면, 오늘 총 인증한 사람 수를 업데이트 한다.
     */
    public void updateTodaySuccessCount(Challenge challenge, LocalDate today) {
        challenge.increaseTodaySuccess(today);
    }

    /*
        피드를 조회하기 위해 사용하는 함수로, 챌린지 id 여러 개와 유저 id로 챌린지 참여 DB를 검색해 성공 일수를 조회한다.
     */
    public List<Object[]> findSuccessDays(Long userId, List<Long> challengeIds) {
        return challengeParticipantRepository.findSuccessDays(userId, challengeIds);
    }

    /*
        해당 유저의 챌린지가 진행중인 챌린지인지 검사한다.
     */
    public void challengeStatusInProgress(Long userId, Long challengeId) {
        ChallengeParticipant challengeParticipant = findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);
        if (challengeParticipant.getStatus() != Status.IN_PROGRESS) {
            throw new ChallengeNotInProgressException("CHALLENGE_IS_NOT_IN_PROGRESS");
        }
    }

    /*
        두 날짜 사이에 걸쳐 있는 캘린더 주차의 개수를 계산하는 함수이다.
     */
    public int calculateWeekIndex(LocalDate start, LocalDate end) {
        WeekFields wf = WeekFields.ISO;  // ISO 주차 규칙을 사용하는 기준점이다.

        LocalDate cursor = start.with(wf.dayOfWeek(), 1);   // 해당 날짜의 시작 주에서 월요일을 기준으로 한다.
        LocalDate endWeek = end.with(wf.dayOfWeek(), 1);  // 위와 같은 일을 한다. 따라서 end가 화요일이면 월요일로 시간을 바꿔주는 역할을 한다.

        int weeks = 1;
        while (cursor.isBefore(endWeek)) {  // 몇 주가 차이나는지 주 단위로 이동하며 계산한다.
            cursor = cursor.plusWeeks(1);
            weeks++;
        }

        return weeks;
    }

    /*
        유저가 해당 날짜에 로그가 하나라도 존재하는지 검색하는 함수이다.
     */
    public boolean existsByUser_IdAndPostDateForLog(Long userId, LocalDate postDate) {
        return challengeDailyLogRepository.existsByUser_IdAndPostDate(userId, postDate);
    }

    /*
        유저 id와 챌린지 id로 날짜 사이에 존재하는 로그 개수를 반환하는 함수이다.
     */
    public long countByUser_IdAndChallenge_IdAndPostDateBetween(Long userId, Long challengeId, LocalDate start, LocalDate end) {
        return challengeDailyLogRepository.countByUser_IdAndChallenge_IdAndPostDateBetween(userId, challengeId, start, end);
    }

    /*
        유저 id, 챌린지 id로 해당 챌린지의 주간 상태에서 주마다의 인증 횟수를 리스트로 반환하는 함수이다.
     */
    public List<Integer> findRecentSuccessCounts(Long userId, Long challengeId, Pageable pageable) {
        return challengeWeeklyStatusRepository.findRecentSuccessCounts(userId, challengeId, pageable);
    }
}
