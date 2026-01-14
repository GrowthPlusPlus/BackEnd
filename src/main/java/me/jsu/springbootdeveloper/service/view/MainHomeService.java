// 정성우

package me.jsu.springbootdeveloper.service.view;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.*;
import me.jsu.springbootdeveloper.dto.mainHome.MainHomeResponse;
import me.jsu.springbootdeveloper.dto.mainHome.MyChallenge;
import me.jsu.springbootdeveloper.repository.*;
import me.jsu.springbootdeveloper.service.NotificationService;
import me.jsu.springbootdeveloper.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/*
    메인화면 구성을 위한 정보를 보내주기 위해 정보들을 정제해주는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 해당 서비스는 조회 전용이다.
public class MainHomeService {
    private final UserService userService;
    private final NotificationService notificationService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeDailyLogRepository challengeDailyLogRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;

    public MainHomeResponse getMainHome(String userEmail, LocalDate today, LocalDate weekStartDate) {
        User user = userService.findByEmail(userEmail);
        List<MyChallenge> myChallenges;  // 유저의 챌린지 별 정보가 들어갈 변수이다.

        // 유저 id로 읽지 않은 알람 개수를 가져온다.
        long notificationNumber = notificationService.countByUser_IdAndIsReadFalse(user.getId());

        // 내가 참여한 챌린지 목록을 가져온다.
        List<ChallengeParticipant> joinedChallenges = challengeParticipantRepository.findWithChallengeByUserId(user.getId());
        for (ChallengeParticipant challengeParticipant : joinedChallenges) {

            // 오늘 데일리 로그 처리

            // 이번주 성공 여부 처리

        }

        return new MainHomeResponse(
                myChallenges,
                notificationNumber
        );
    }
}