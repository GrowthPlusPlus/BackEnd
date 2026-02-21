// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import me.jsu.springbootdeveloper.challenge.dto.*;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInviteLink;
import me.jsu.springbootdeveloper.challenge.enumType.ChallengeVisibility;
import me.jsu.springbootdeveloper.challenge.enumType.InviteLinkType;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeFriendInviteListResponse;
import me.jsu.springbootdeveloper.challenge.dto.ChallengeInviteResponse;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeInvite;
import me.jsu.springbootdeveloper.challenge.exception.*;
import me.jsu.springbootdeveloper.challenge.repository.*;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeMemberService;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeQueryService;
import me.jsu.springbootdeveloper.challenge.service.temp.ChallengeUtils;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.repository.FriendshipRepository;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
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
import me.jsu.springbootdeveloper.tag.dto.TagResponse;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.tag.service.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import me.jsu.springbootdeveloper.user.dto.FriendListResponse;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.service.FriendService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeMemberService challengeMemberService;

    /*
        챌린지의 상세 정보를 반환하는 함수이다.
     */
    public ChallengeDetailResponse getChallengeDetail(Long challengeId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 챌린지의 태그를 조회한다.
        List<TagToChallenge> tags = tagService.searchChallengeTags(challengeId);
        List<String> tagNames = tags.stream()
                .map(tc -> tc.getTag().getTagName())  // 태그 이름만 추출한다.
                .toList();

        // 오늘 인증한 유저 정보를 조회한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<User> successUsers = challengeQueryService.getTodaySuccessUsers(challengeId, today);

        return new ChallengeDetailResponse(
                challenge.getTitle(),
                challenge.getStartDate(),
                challenge.getEndDate(),
                challenge.getRequiredWeeklyCount(),
                challenge.isPhotoRequired(),
                tagNames,
                challenge.getContent(),
                new ChallengeDetailResponse.HostInfo(
                        challenge.getUser().getNickname(),
                        challenge.getUser().getProfileImageUrl()
                ),
                challenge.getParticipantNumber(),  // 참여자 수
                successUsers.stream()
                        .map(u -> new ChallengeDetailResponse.UserSummary(
                                u.getNickname(),
                                u.getProfileImageUrl()
                        ))
                        .toList()
        );
    }

    // 챌린지 생성 기능
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
        joinChallenge(savedChallenge, user);

        // 챌린지 생성 시 자동으로 초대 링크 생성
        // 기본 설정: TIME_BASED 타입, 30일 후 만료
        String inviteLink = createInviteLink(savedChallenge, user);

        //친구 목록 가져오기
        List<FriendListResponse> friends = friendService.getFriendList(email);

        //(1) url 링크, (2)친구 목록 반환하기
        return new CreateChallengeResponse(savedChallenge,inviteLink, friends);

    }
    /*
        친구를 챌린지에 초대하는 함수이다.
        (1) 알림 전송 (2) 초대 테이블에 'INVITED' 상태로 저장
     */
    @Transactional
    public void inviteUser(String inviterEmail, Long challengeId, String targetNickname) {
        User inviter = userService.findByEmail(inviterEmail);
        Challenge challenge = challengeQueryService.findById(challengeId);
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

    /*
        챌린지에 참여하는 컨트롤러와 연결된 함수이다.
        코드를 재사용하기 위해 사용한다.
     */
    public void joinChallengeController(String email, Long challengeId) {
        User user = userService.findByEmail(email);
        Challenge challenge = challengeQueryService.findById(challengeId);

        joinChallenge(challenge, user);  // 챌린지에 실제로 참여한다.
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
     * 초대 링크도 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public ChallengeInviteResponse getFriendList_with_InviteStatus(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        // 전체 친구 목록을 조회한다. 유저 프로필까지 한 번에 조회한다.
        List<User> friends = friendshipRepository.findAllFriendsByUserId(user.getId());

        // 친구의 id를 수집한다.
        List<Long> friendIds = friends.stream()
                .map(User::getId)
                .toList();

        // 수집한 친구 id들 중, 해당 챌린지에 참여 중인 친구 id를 다시 수집한다.
        Set<Long> participantIds = new HashSet<>(challengeParticipantRepository.findParticipantUserIdsIn(challengeId, friendIds));

        // 이미 초대한 친구들의 id를 수집한다.
        Set<Long> invitedIds = new HashSet<>(challengeInviteRepository.findInvitedUserIdsIn(challengeId, friendIds));

        // 친구 목록과 초대 상태를 조회한다.
        List<ChallengeFriendInviteListResponse> friendList = friends.stream()
                .filter(friend-> !participantIds.contains(friend.getId()))  // 이미 참여 중인 친구는 제외한다.
                .map(friend -> {
                    // 친구의 프로필 사진을 조회한다.
                    UserProfilePicture picture = friend.getUserProfilePicture();
                    String profileUrl = (picture != null) ? picture.getImageUrl() : null;

                    // 해당 친구의 초대 상태 판별 (1. 초대되거나 2.초대되지 않음 (해당 초대 테이블에 저장된 정보 없음))
                    boolean inviteStatus = invitedIds.contains(friend.getId());
                    return new ChallengeFriendInviteListResponse(friend, profileUrl, inviteStatus);
                })
                .collect(Collectors.toList());

        // 챌린지 초대 링크를 조회한다(1개라고 가정)
        // 유효한 챌린지 링크가 존재하지 않는다면 새로 생성하여 반환한다.
        Optional<ChallengeInviteLink> challengeInviteLink = challengeInviteLinkRepository.findFirstValidLinkByChallengeId(challengeId, LocalDateTime.now());
        
        String link;
        ChallengeInviteLink inviteLink;
        if(challengeInviteLink.isPresent()){
            Challenge challenge = challengeQueryService.findById(challengeId);
            inviteLink = challengeInviteLink.get();

            link = baseUrl + "/v/"+ inviteLink.getInviteCode();
        }
        else{
            Challenge challenge1 = challengeQueryService.findById(challengeId);

            link = createInviteLink(challenge1, user);
        }
        

        // 초대 링크와 친구 목록을 함께 반환한다.
        return new ChallengeInviteResponse(link, friendList);
    }

    /**
     * 키워드로 챌린지를 검색한다. (제목 또는 태그명 포함)
     */
    public Page<SearchChallengeResponse> searchChallenges(String keyword, int page) {
        Pageable pageable = PageRequest.of(page, 20);

        // 검색어가 포함된 공개 챌린지를 조회한다.
        Page<Challenge> challenges = challengeRepository.searchChallenges(keyword, ChallengeVisibility.PUBLIC, pageable);

        // 검색한 챌린지의 id 목록을 추출한다.
        List<Long> challengeIds = challenges.getContent()
                .stream()
                .map(Challenge::getId)
                .toList();

        // 태그를 한 번에 조회한다.
        Map<Long, List<TagResponse>> tagMap = tagService.getChallengeTagsByOnce(challengeIds);

        // DTO로 변환한다. (생성자에서 필요한 모든 필드 매핑)
        return challenges.map(challenge -> {
            // 맞는 챌린지 id키가 있으면 태그 리스트를 생성하고 없다면 빈 리스트를 생성한다.
            List<TagResponse> tags = tagMap.getOrDefault(challenge.getId(), List.of());

            return new SearchChallengeResponse(challenge, tags);
        });
    }

    /*
        사용자에게 기존 챌린지 설정 정보를 반환하는 함수이다.
     */
    public ChallengeSettingsResponse getSettings(Long challengeId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

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
                String content, boolean photoRequired, ChallengeVisibility challengeVisibility)
    {
        User user = userService.findByEmail(email);
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 방장인지 검증한다.
        challengeMemberService.validateOwner(challengeMemberService.isOwner(user.getId(), challengeId));

        // int로 들어온 종료일을 LocalDate 형식으로 변환한다.
        LocalDate endDate = challenge.getStartDate().plusDays(endDateInteger);

        tagService.deleteAllChallenge_Id(challengeId);  // 기존 챌린지-태그 매핑을 모두 삭제한다.

        // 요청 태그로 업데이트 한다.
        tagService.updateChallengeTags(tagIds, challenge);

        // 설정 값을 업데이트 한다. content값은 null이 될 수 있다.
        challenge.updateSettings(endDate, requiredWeeklyCount, content, photoRequired, challengeVisibility);
    }

    /*
        특정 챌린지를 완수한 경우, 연속 챌린지 달성 일수와 총 챌린지 일수를 업데이트하기 위해 호출하는 함수이다.
        반드시 하루에 최대 한 번만 수행한다.
     */
    public void succeedChallenge(Long userId, Long challengeId, LocalDate postDate) {
        // 챌린지 참여 DB를 검색한다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);
        challengeParticipant.recordDailySuccess(postDate);  // 챌린지 참여 DB를 업데이트 한다.
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
        인증글이 생성되면, 오늘 총 인증한 사람 수를 업데이트 한다.
     */
    public void updateTodaySuccessCount(Challenge challenge, LocalDate today) {
        challenge.increaseTodaySuccess(today);
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
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challenge.getId());

        // 이번 주차가 챌린지 시작 이후 몇 주차인지 검색한다.
        int weekIndex = ChallengeUtils.calculateWeekIndex(challengeParticipant.getJoinDate(), weekStartDate);

        // 이번 주의 상태가 성공이라면 총 성공 주차 수를 업데이트 한다. 주에 한 번만 수행된다.
        if (challengeWeeklyStatus.getStatus() == Status.SUCCESS && challengeParticipant.getSuccessWeeks() < weekIndex) {
            challengeParticipant.increaseSuccessWeeks();
        }
    }

    /*
        유저 id, 챌린지 id, 이번주 날짜로 챌린지 주간 상태 값을 리턴하는 함수이다.
        주간 상태 업데이트에 사용된다.
     */
    public ChallengeWeeklyStatus findWeeklyStatus(Long userId, Long challengeId, LocalDate weekStartDate) {
        return challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                        userId, challengeId, weekStartDate)
                .orElse(null);
    }

    /*
        챌린지 초대 링크를 생성하는 헬퍼 메서드이다.
        
        @param challenge: 초대 링크를 생성할 챌린지
        @param user: 링크를 생성하는 사용자 (챌린지 생성자)
        @param linkType: 링크 타입 (SINGLE_USE, TIME_BASED, MULTI_USE)
        @param expirationDays: 만료 일수 (TIME_BASED 타입의 경우 사용)
        @return 생성된 ChallengeInviteLink 객체 (아직 DB에 저장되지 않음)
     */
    private String createInviteLink(Challenge challenge, User user) {
        return createInviteLink(challenge, user, InviteLinkType.TIME_BASED, 30);
    }

    private String createInviteLink(Challenge challenge, User user,
                                    InviteLinkType linkType,int expirationDays) {

        // 고유한 초대 코드 생성 (중복 체크 포함)
        String inviteCode = generateUniqueInviteCode();

        // ChallengeInviteLink 객체 생성
        ChallengeInviteLink challengeInviteLink = ChallengeInviteLink.builder()
                .challenge(challenge)
                .user(user)
                .linktype(linkType)
                .inviteCode(inviteCode)
                .build();

        //링크 타입 - TimeBased 일 경우 만료 시간 계산 (현재 시간 + expirationDays일)
        if (linkType == InviteLinkType.TIME_BASED){
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(expirationDays);
            challengeInviteLink.setExpiresAt(expiresAt);
        }

        // 링크 타입 - MultiUse 일 경우 최대 사용 횟수 설정
        if (linkType == InviteLinkType.MULTI_USE) {
            int maxUses = 10;
            challengeInviteLink.setMaxUses(maxUses);
        }

        // 링크 타입 - SingleUse 일 경우 사용 횟수 설정 및 활성화 해제
        if(linkType == InviteLinkType.SINGLE_USE){
            challengeInviteLink.setUsedCount(1);
            challengeInviteLink.setActive(false);
        }
        // 초대 링크 저장
        challengeInviteLinkRepository.save(challengeInviteLink);
        // 초대 링크 생성
        String InviteLink= baseUrl + "/v/" + challengeInviteLink.getInviteCode();
        return InviteLink;
    }

    /*
        고유한 초대 코드를 생성하는 메서드이다.
        도메인 메서드를 사용하여 초대 코드를 생성하고, 중복 체크를 통해 고유성을 보장한다.

        @param inviteLink: 초대 코드를 생성할 ChallengeInviteLink 객체
     */
    public String generateUniqueInviteCode() {
        int maxAttempts = 100;  // 최대 재시도 횟수이다. 중복 코드가 생성될 경우를 대비하여 재시도한다.
        int attempt = 0;


        //초대 코드 생성
        while (attempt < maxAttempts) {
            // 도메인 메서드를 사용하여 초대 코드 생성 (UUID의 앞 8자리 사용)
            String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            // 중복 체크: 이미 존재하는 코드인지 확인
            if (!challengeInviteLinkRepository.findByInviteCode(inviteCode).isPresent()) {
                return inviteCode;  // 고유한 코드이면 종료
            }

            attempt++;  // 중복이면 재시도
        }

        // 최대 재시도 횟수를 초과한 경우 예외 발생
        // 이는 매우 드문 경우이지만, 코드 길이를 늘리거나 재시도 횟수를 늘려야 할 수 있다.
        throw new IllegalStateException(
                "초대 코드 생성에 실패했습니다. 최대 재시도 횟수(" + maxAttempts + "회)를 초과했습니다."
        );
    }

    /*
        유저 id, 챌린지 id로 해당 챌린지의 주간 상태에서 주마다의 인증 횟수를 리스트로 반환하는 함수이다.
     */
    public List<Integer> findRecentSuccessCounts(Long userId, Long challengeId, Pageable pageable) {
        return challengeWeeklyStatusRepository.findRecentSuccessCounts(userId, challengeId, pageable);
    }
}
