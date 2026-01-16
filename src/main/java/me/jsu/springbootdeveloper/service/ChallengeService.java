// 정성우

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Challenge;
import me.jsu.springbootdeveloper.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.repository.ChallengeDailyLogRepository;
import me.jsu.springbootdeveloper.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.repository.ChallengeWeeklyStatusRepository;
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
}
