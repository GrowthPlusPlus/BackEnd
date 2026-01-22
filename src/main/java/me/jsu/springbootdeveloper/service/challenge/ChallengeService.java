// 정성우

package me.jsu.springbootdeveloper.service.challenge;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.*;
import me.jsu.springbootdeveloper.domain.post.Article;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.domain.user.UserProfilePicture;
import me.jsu.springbootdeveloper.dto.challenge.ChallengeMemberResponse;
import me.jsu.springbootdeveloper.dto.tag.TagResponse;
import me.jsu.springbootdeveloper.repository.challenge.*;
import me.jsu.springbootdeveloper.repository.post.PostRepository;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.dto.challenge.*;
import me.jsu.springbootdeveloper.service.tag.TagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

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

    /*
        챌린지 id로 챌린지를 검색해주는 함수이다.
     */
    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_NOT_FOUND_FOR_ID"));
    }

    /*
        유저 id로 유저가 참여한 챌린지 중, 현재 진행중인 챌린지를 반환해주는 함수이다.
     */
    public List<ChallengeParticipant> findInProgressWithChallengeByUserId(Long userId) {
        return challengeParticipantRepository.findInProgressWithChallengeByUserId(userId);
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
        챌린지 데일리 로그의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachLogToUser(User user) {
        challengeDailyLogRepository.detachUser(user);
    }

    /*
        챌린지 데일리 로그의 인증글 fk값을 null로 바꿔주는 함수이다.
        인증글 삭제 시에 사용한다.
     */
    @Transactional
    public void detachLogToArticle(Article article) {
        challengeDailyLogRepository.detachArticle(article);
    }

    /*
        특정 챌린지를 완수한 경우, 연속 챌린지 달성 일수와 총 챌린지 일수를 업데이트하기 위해 호출하는 함수이다.
        반드시 하루에 최대 한 번만 수행한다.
     */
    @Transactional
    public void succeedChallenge(Long userId, Long challengeId) {
        // 챌린지 참여 DB를 검색한다.
        ChallengeParticipant challengeParticipant = findByUser_IdAndChallenge_IdForParticipant(userId, challengeId);
        challengeParticipant.recordDailySuccess(LocalDate.now());  // 챌린지 참여 DB를 업데이트 한다.
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

        Page<ChallengeParticipant> challengeParticipants =
                challengeParticipantRepository.findMembersWithUserAndPicture(
                        challengeId, (nickname == null || nickname.isBlank()) ? null : nickname, pageable
                );

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
    public void delegateOwner(String email, Long challengeId, Long targetUserId, boolean leaveAfterDelegate) {
        User user = userService.findByEmail(email);

        // 챌린지를 조회한다.
        Challenge challenge = findById(challengeId);

        // 방장 권한을 검증한다.
        validateOwner(isOwner(user.getId(), challengeId));

        // 위임 대상자가 참여자가 맞는지 검증하고 검색한다.
        ChallengeParticipant targetParticipant = findByUser_IdAndChallenge_IdForParticipant(targetUserId, challengeId);

        // 방장을 변경한다.
        challenge.updateUser(targetParticipant.getUser());

        // 이전의 방장이 챌린지에 남을지, 나갈지 선택한다.
        if (leaveAfterDelegate) {
            leaveChallengeInternal(user.getId(), challengeId);
        }
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
        인증글이 생성되었을 때, 챌린지 주간 상태를 업데이트 한다.
        월요일을 기준으로 이번주에 처음 인증을 하면 그 주에 해당하는 챌린지 주간 상태값이 생성된다.
        만약 주의 첫 인증이 아니라면 값을 업데이트 한다.
     */
    public void updateWeeklyStatus(User user, Challenge challenge, LocalDate weekStartDate, int requiredWeeklyCount) {
        // 이번주에 해당하는 챌린지 주간 상태 튜플이 있는지 검색한다.
        Optional<ChallengeWeeklyStatus> optionalStatus = challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                                user.getId(),
                                challenge.getId(),
                                weekStartDate
        );
        if (optionalStatus.isPresent()) {  // 이번 주 상태가 이미 있으면 업데이트 한다.
            ChallengeWeeklyStatus weeklyStatus = optionalStatus.get();
            weeklyStatus.updateSuccessCount(requiredWeeklyCount);
        }
        else {  // 이번 주 첫 인증이면 새로 생성한다.
            ChallengeWeeklyStatus weeklyStatus = ChallengeWeeklyStatus.builder()
                            .user(user)
                            .challenge(challenge)
                            .weekStartDate(weekStartDate)
                            .build();

            weeklyStatus.updateSuccessCount(requiredWeeklyCount);
            challengeWeeklyStatusRepository.save(weeklyStatus);
        }
    }
}
