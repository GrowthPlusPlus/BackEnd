// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.exception.NotAllowedChallengeActionException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeDailyLogRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.post.domain.Article;
import me.jsu.springbootdeveloper.post.domain.PostPicture;
import me.jsu.springbootdeveloper.post.event.CloudinaryDeleteEvent;
import me.jsu.springbootdeveloper.post.repository.PostPictureRepository;
import me.jsu.springbootdeveloper.post.repository.PostRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/*
    챌린지와 관련된 값을 삭제하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class ChallengeDeleteService {
    private final ChallengeQueryService challengeQueryService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeDailyLogRepository challengeDailyLogRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final PostRepository postRepository;
    private final PostPictureRepository postPictureRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final UserService userService;
    private final UserStatisticsService userStatisticsService;

    /*
        유저가 회원탈퇴를 하는 경우, 해당 유저가 오너인 챌린지가 삭제되지 않도록 다른 유저에게 위임한다.
        방장 외에 유저가 없다면 챌린지를 삭제한다.
     */
    public void handleOwnerWithdrawal(Long userId) {
        List<Long> toDelete = new ArrayList<>();  // 삭제할 챌린지들의 id를 모은 배열이다.
        List<Challenge> challenges = challengeQueryService.findByUser_Id(userId);  // 현재 관리하는 챌린지들을 검색한다.

        if (challenges.isEmpty()) {  // 관리하는 챌린지가 없다면 리턴한다.
            return;
        }

        // 관리하는 챌린지의 id들을 따로 map으로 만든다.
        List<Long> challengeIds = challenges.stream()
                .map(Challenge::getId)
                .toList();

        // 챌린지 오너를 제외한 챌린지 참가자들을 검색한다.
        List<ChallengeParticipant> allParticipants = challengeParticipantRepository.findParticipantsExceptOwnerForChallenges(challengeIds, userId, Status.IN_PROGRESS);

        // 검색한 참가자들의 정보를 challengeId를 기준으로 그룹화한다.
        Map<Long, List<ChallengeParticipant>> grouped = allParticipants.stream()
                        .collect(Collectors.groupingBy(cp -> cp.getChallenge().getId()));

        // 챌린지별로 위임할 참가자들을 선택한다.
        for (Challenge challenge : challenges) {
            // 위임할 참가자를 검색한다.
            List<ChallengeParticipant> participants = grouped.getOrDefault(challenge.getId(), List.of());

            if (participants.isEmpty()) {  // 위임할 참가자가 없다면 챌린지를 삭제 목록에 추가한다.
                toDelete.add(challenge.getId());
            }
            else {  // 스코어가 가장 높은 사람에게 위임한다.
                User newOwner = selectNextOwner(participants);

                challenge.updateUser(newOwner);
            }
        }

        // 반복이 끝난 후 챌린지를 삭제한다.
        for (Long id : toDelete) {
            deleteChallengeInternal(id);
        }
    }

    /*
        챌린지를 실제로 삭제하는 서비스 클래스이다. 내부적으로 재사용하기 위해 분리하여 사용한다.
        데일리 로그, 인증글은 FK만 null 처리하고, 나머지 종속된 DB값들은 cascade 옵션으로 자동 삭제한다.
     */
    public void deleteChallengeInternal(Long challengeId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 삭제 전에 잔디의 재집계를 위해 챌린지에 참여한 유저 id를 저장한다.
        List<Long> userIds = challengeQueryService.findUserIdsByChallengeId(challengeId);

        /* 기존 방식은 null 처리한다.
        // 데일리 로그, 인증글의 fK값을 null 처리한다.
        challengeDailyLogRepository.detachChallenge(challenge);
        postRepository.detachChallenge(challenge);  // 순환참조 방지를 위해 서비스를 사용하지 않고 직접 사용한다.
        */

        // 챌린지에 속한 로그와 인증글을 삭제한다.
        List<Article> articles = postRepository.findByChallengeId(challengeId);
        for (Article article : articles) {
            deletePostAndLog(article);
        }

        // 챌린지를 삭제한다. 나머지 종속된 값들은 cascade REMOVE 옵션으로 자동 삭제된다.
        challengeRepository.delete(challenge);

        // 삭제된 챌린지에 참여했던 유저들의 잔디를 재집계 한다.
        for (Long userId : userIds) {
            userStatisticsService.researchLog(userId, LocalDate.now(ZoneId.of("Asia/Seoul")));
        }
    }

    /*
        챌린지를 삭제하는 서비스 클래스이다.
        컨트롤러-서비스 구조를 유지하기 위해 사용한다.
     */
    @Transactional
    public void deleteChallenge(String email, Long challengeId) {
        User user = userService.findByEmail(email);

        // 방장 권한을 검증한다.
        if(!challengeRepository.existsByIdAndUser_Id(challengeId, user.getId())) {
            throw new NotAllowedChallengeActionException("NOT_CHALLENGE_OWNER");
        }

        // 챌린지를 삭제한다.
        deleteChallengeInternal(challengeId);
    }

    /*
        모든 챌린지에서 유저가 나올때 사용하는 함수이다. 회원탈퇴 시에 사용된다.
        해당 함수는 방장 위임이 끝난 뒤에 사용해야 한다.
     */
    public void deleteParticipant(List<Long> userIds) {
        // 유저 아이디로 해당 유저가 참가한 챌린지들을 검색한다.
        List<ChallengeParticipant> participants = challengeQueryService.findAllByUserIds(userIds);
        if (participants.isEmpty()) {  // 참여한 챌린지가 없다면 리턴한다.
            return;
        }

        challengeParticipantRepository.deleteAllInBatch(participants);  // 챌린지 참여에서 삭제한다.

        for (ChallengeParticipant challengeParticipant : participants) {
            updateChallengeParticipantCount(challengeParticipant.getChallenge());  // 챌린지 참여자 수를 감소시킨다.
        }
    }

    /*
        챌린지 데일리 로그를 실제로 삭제한다.
        인증글을 삭제하는 경우에 사용한다.
     */
    public void deleteLog(ChallengeDailyLog challengeDailyLog) {
        challengeDailyLogRepository.delete(challengeDailyLog);
    }

    /*
        챌린지 데일리 로그의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    public void detachLogToUser(List<Long> userIds) {
        challengeDailyLogRepository.detachUser(userIds);
    }

    /*
        관리자가 방장 여부와 관계없이 챌린지를 삭제할 때 사용한다.
        @param challengeId 삭제할 챌린지 id
     */
    @Transactional
    public void deleteChallengeAsAdmin(Long challengeId) {
        deleteChallengeInternal(challengeId);
    }

    /*
        정합성을 유지하기 위해 챌린지 참여자 수를 감소시키는 함수이다.
        만약 챌린지 도메인 내의 참여자 수와 실제 참여자 수가 다른 경우 다시 계산해주는 역할을 한다.
     */
    public void updateChallengeParticipantCount(Challenge challenge) {
        int participantNumber = (int) challengeParticipantRepository.countByChallenge_Id(challenge.getId());  // 전체 참여자 수를 계산한다.

        challenge.synchronizeParticipantNumber(participantNumber);
    }

    /*
        기준에 따라 다음 방장이 될 유저를 선택하는 함수이다.
        현재는 랭킹이 가장 높은 유저를 선택한다.
     */
    public User selectNextOwner(List<ChallengeParticipant> participants) {
        return participants.stream()
                .max(Comparator.comparing(ChallengeParticipant::getRankingScore))
                .map(ChallengeParticipant::getUser)
                .orElse(null);
    }

    /*
        챌린지 삭제 시 인증글과 로그를 모두 삭제하기 위한 함수이다.
     */
    public void deletePostAndLog(Article article) {
        // 인증글의 사진 정보를 검색한다.
        List<PostPicture> postPictures = postPictureRepository.findAllByArticle_Id(article.getId());

        // 인증글에 저장된 사진들을 클라우디너리에서 삭제한다. 이미지가 없는 경우엔 if문을 실행하지 않는다.
        if (!postPictures.isEmpty()) {
            for (PostPicture pp : postPictures) {
                String publicId = pp.getCloudinaryPublicId();

                // 우리 서버의 DB에서 정보를 삭제한다.
                postPictureRepository.delete(pp);

                // 클라우디너리의 이미지를 삭제한다. 모든 트렌젝션이 끝난 후에 수행한다.
                applicationEventPublisher.publishEvent(new CloudinaryDeleteEvent(publicId));
            }
        }

        // 게시글 작성자가 탈퇴한 경우엔 게시글만 삭제한다.
        if (article.getUser() == null) {
            postRepository.delete(article);
            return;
        }

        Long authorId = article.getUser().getId();  // 인증글 작성자의 id를 저장한다.

        LocalDate postDate = article.getPostDate();  // 인증글의 생성 날짜를 검색한다.
        LocalDate todaySeoul = LocalDate.now(ZoneId.of("Asia/Seoul"));  // 삭제 당일 시간을 검색한다.
        boolean isTodayPost = todaySeoul.isEqual(postDate);  // 두 날짜가 같은지 확인한다.

        // 챌린지 데일리 로그 정보는 게시글이 삭제되면 삭제한다.
        ChallengeDailyLog challengeDailyLog = challengeQueryService.findLogByUser_IdAndArticle_IdAndPostDate(authorId, article.getId(), postDate);
        deleteLog(challengeDailyLog);

        // 삭제된 로그를 기반으로 연간 활동(잔디)를 업데이트한다. 위에서 마지막에 총괄적으로 하므로 필요 없음
        //userStatisticsService.researchLog(authorId, postDate);

        Challenge challenge = article.getChallenge();  // 인증글의 챌린지 정보를 가져온다.

        // 당일 글일 때만 챌린지 "오늘 성공" 카운터를 줄인다.
        if (isTodayPost) {
            challenge.decreaseTodaySuccess();
        }

        // 해당 유저의 총 챌린지 성공 일수를 감소시킨다.
        ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(authorId, challenge.getId());
        challengeParticipant.removeDailySuccess(postDate);

        // 이번주 챌린지 총 성공 횟수를 감소시킨다.
        LocalDate thisWeekMonday = postDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        ChallengeWeeklyStatus challengeWeeklyStatus = challengeQueryService.findWeeklyStatus(authorId, challenge.getId(), thisWeekMonday);

        // 부분 주를 적용한다.
        int adjustedRequired = ChallengeUtils.calculateAdjustedRequired(
                challenge,
                thisWeekMonday,
                thisWeekMonday.plusDays(6),
                challengeParticipant.getJoinDate()
        );

        challengeWeeklyStatus.decreaseSuccessCount(adjustedRequired);

        if (isTodayPost) {  // 삭제 날짜가 생성 날짜와 같다면 기존과 동일하게 유저 통계의 내용을 롤백한다.
            userStatisticsService.onTodayPostCancelled(authorId, challenge.getId(), postDate);
        }
        else {
            /*
                과거 글: unmarkActiveToday는 슬라이딩 윈도우가 과거 날짜를 가정하지 않으므로 쓰지 않는다.
                로그 삭제·참가자 롤백 반영 후 남은 데일리 로그로 UserStatistics를 다시 맞춘 뒤 인증글을 삭제한다.
             */
            userStatisticsService.resyncUserStatisticsAfterPastPostDeletion(authorId);
            postRepository.delete(article);
            return;
        }

        postRepository.delete(article);  // DB에서 게시글을 삭제한다. 댓글이나 좋아요 등의 정보도 함께 삭제된다.
    }
}
