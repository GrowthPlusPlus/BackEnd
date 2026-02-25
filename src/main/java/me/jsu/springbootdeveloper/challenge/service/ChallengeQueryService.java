// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotFoundException;
import me.jsu.springbootdeveloper.challenge.exception.ChallengeNotInProgressException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeDailyLogRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeWeeklyStatusRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/*
    챌린지와 관련된 DB 검색을 수행하는 서비스 클래스이다. 짧은 코드의 함수가 주로 존재한다.
 */
@RequiredArgsConstructor  // final 또는 notNull이 있는 맴버변수만 생성자에서 초기화 한다.
@Service
public class ChallengeQueryService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeDailyLogRepository challengeDailyLogRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;

    /*
        챌린지 id로 챌린지를 검색해주는 함수이다.
     */
    public Challenge findById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_NOT_FOUND_FOR_ID"));
    }

    /*
        유저 id로 유저가 참여한 모든 진행중, 성공, 실패 챌린지 목록을 가져오는 함수이다. 챌린지 정보도 포함하여 가져온다.
     */
    public  List<ChallengeParticipant> findWithChallengeByUserId(Long userId) {
        return challengeParticipantRepository.findWithChallengeByUserId(userId);
    }

    /*
        유저 id와 챌린지 상태로 참여한 챌린지를 최신순으로 반환해주는 함수이다. 챌린지 정보도 포함하여 리턴한다.
     */
    public List<ChallengeParticipant> findStatusWithChallengeByUserIdAndStatus(Long userId, Status status) {
        return challengeParticipantRepository.findStatusWithChallengeByUserIdAndStatus(userId, status);
    }

    /*
        유저 id 리스트와 챌린지 상태로 검색한다. 위 함수와의 차이는 유저 id를 리스트로 매개변수로 준다.
     */
    public List<ChallengeParticipant> findAllByUserIdsAndStatus(List<Long> userIds, Status status) {
        return challengeParticipantRepository.findAllByUserIdsAndStatus(userIds, status);
    }

    /*
        오늘 해당 챌린지를 수행한 유저를 로그의 존재 여부로 조회한다.
        오늘 인증한 유저를 찾을 때 사용한다.
     */
    public List<User> getTodaySuccessUsers(Long challengeId, LocalDate today) {
        return challengeDailyLogRepository.findTodaySuccessUsers(challengeId, today);
    }

    /*
        유저 id와 날짜로 해당 날짜에 수행한 데일리 로그를 반환해주는 함수이다.
        mainHome 서비스에서 로그를 조회하기 위해 사용한다.
     */
    public List<ChallengeDailyLog> findByUser_IdAndPostDate(Long userId, LocalDate targetDate) {
        return challengeDailyLogRepository.findByUser_IdAndPostDate(userId, targetDate);
    }

    /*
        유저 id와 이번주 시작일로 해당 주에 유저가 챌린지를 몇 회 수행했는지 등에 관한 정보를 반환해주는 함수이다.
        mainHome 서비스에서 챌린지 실패 위험 여부를 판단하기 위해 사용한다.
     */
    public List<ChallengeWeeklyStatus> findByUser_IdAndWeekStartDate(Long userId, LocalDate weekStartDate) {
        return challengeWeeklyStatusRepository.findByUser_IdAndWeekStartDate(userId, weekStartDate);
    }

    /*
        해당 유저가 방장인 모든 챌린지를 검색한다.
        회원탈퇴를 하는 경우, 방장을 위임하기 위해 사용한다.
     */
    public List<Challenge> findByUser_Id(Long userId) {
        return challengeRepository.findByUser_Id(userId);
    }

    /*
        챌린지 데일리 로그를 유저 id, 인증글 id, 날짜로 검색한다.
        인증글을 삭제하는 경우, 로그도 삭제하기 위해 사용한다.
     */
    public ChallengeDailyLog findLogByUser_IdAndArticle_IdAndPostDate(Long userId, Long articleId, LocalDate postDate) {
        return challengeDailyLogRepository.findByUser_IdAndArticle_IdAndPostDate(userId, articleId, postDate)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_LOG_NOT_FOUND"));
    }

    /*
        챌린지 참여자 DB값을 챌린지 id, 유저 id로 검색한다.
     */
    public ChallengeParticipant findByUser_IdAndChallenge_IdForParticipant(Long userId, Long challengeId) {
        return challengeParticipantRepository.findByUser_IdAndChallenge_Id(userId, challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException("CHALLENGE_PARTICIPANT_NOT_FOUND"));
    }

    /*
        유저가 해당 날짜에 로그가 하나라도 존재하는지 검색하는 함수이다.
     */
    public boolean existsByUser_IdAndPostDateForLog(Long userId, LocalDate postDate) {
        return challengeDailyLogRepository.existsByUser_IdAndPostDate(userId, postDate);
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
        피드를 조회하기 위해 사용하는 함수로, 챌린지 id 여러 개와 유저 id로 챌린지 참여 DB를 검색해 성공 일수를 조회한다.
     */
    public List<Object[]> findSuccessDays(List<Long> userIds, List<Long> challengeIds) {
        return challengeParticipantRepository.findSuccessDays(userIds, challengeIds);
    }

    /*
        유저 id, 챌린지 id로 해당 챌린지의 주간 상태에서 주마다의 인증 횟수를 리스트로 반환하는 함수이다.
        유저 통계를 업데이트하기 위해 사용한다.
     */
    public List<Object[]> findRecentSuccessCountsForUsers(List<Long> userId, LocalDate cutoffDate) {
        return challengeWeeklyStatusRepository.findRecentSuccessCountsForUsers(userId, cutoffDate);
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
}
