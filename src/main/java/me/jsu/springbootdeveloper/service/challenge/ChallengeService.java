// 정성우

package me.jsu.springbootdeveloper.service.challenge;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeDailyLog;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeDailyLogRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeWeeklyStatusRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

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
                challengeRepository.delete(challenge);
                continue;  // 다음 챌린지를 검색한다.
            }

            // 가장 먼저 참여한 사람에게 권한을 위임한다.
            User newMaster = participants.get(0).getUser();
            challenge.updateUser(newMaster);
        }
    }
}
