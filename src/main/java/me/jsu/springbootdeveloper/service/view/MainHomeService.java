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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<MyChallenge> myChallenges = new ArrayList<>();  // 유저의 챌린지 별 정보가 들어갈 변수이다.

        // 유저 id로 읽지 않은 알람 개수를 가져온다.
        long notificationNumber = notificationService.countByUser_IdAndIsReadFalse(user.getId());

        // 내가 참여한 챌린지 중, 현재 진행중인 챌린지 목록을 가져온다.
        List<ChallengeParticipant> joinedChallenges = challengeParticipantRepository.findInProgressWithChallengeByUserId(user.getId());
        if (joinedChallenges.isEmpty()) {  // 아직 아무런 챌린지에도 참여하지 않는 경우 빈 리스트를 반환한다.
                return new MainHomeResponse(Collections.emptyList(), notificationNumber);
        }

        // 유저의 오늘 하루 데일리 로그를 검색한다. 하나도 수행하지 않았다면 빈 리스트를 반환한다.
        List<ChallengeDailyLog> challengeDailyLogs = challengeDailyLogRepository.findByUser_IdAndPostDate(user.getId(), today);
        // 데일리 로그 list를 map으로 바꾸어서 챌린지 id와 데일리 로그의 챌린지 id를 바로 매핑한다.
        Map<Long, ChallengeDailyLog> dailyLogMap = challengeDailyLogs.stream().collect(Collectors.toMap(
                log -> log.getChallenge().getId(),  // 챌린지 id를 map key로 설정한다.
                log -> log
        ));

        // 유저의 이번주 챌린지 상태를 검색한다. 이번주에 한 번도 수행하지 않았다면 빈 리스트를 반환한다.
        List<ChallengeWeeklyStatus> challengeWeeklyStatuses = challengeWeeklyStatusRepository.findByUser_IdAndWeekStartDate(
                user.getId(), weekStartDate);
        // 위와 마찬가지로 챌린지 id를 매핑하기 위해 map을 사용한다.
        Map<Long, ChallengeWeeklyStatus> weeklyStatusMap = challengeWeeklyStatuses.stream().collect(Collectors.toMap(
                ws -> ws.getChallenge().getId(),
                ws -> ws
        ));

        for (ChallengeParticipant challengeParticipant : joinedChallenges) {
            Challenge challenge = challengeParticipant.getChallenge();  // 챌린지 하나를 가져온다.
            Long challengeId = challenge.getId();

            // 오늘 데일리 로그 여부를 판별한다.
            ChallengeDailyLog dailyLog = dailyLogMap.get(challengeId);

            boolean isDoIt;
            if (dailyLog == null) {
                isDoIt = false;   // 오늘 인증을 하지 않은 경우이다.
            }
            else {
                isDoIt = true;    // 로그가 존재하여 오늘 인증을 한 경우이다.
            }

            // 이번 주 챌린지가 실패에 임박했는지 판단한다.
            ChallengeWeeklyStatus weeklyStatus = weeklyStatusMap.get(challengeId);

            // 이번 주 성공 횟수를 계산한다. weeklyStatus 값 자체가 존재하지 않으면 0회로 간주한다.
            int successCount = (weeklyStatus == null) ? 0 : weeklyStatus.getSuccessCount();

            // 챌린지의 주당 최소 인증 횟수를 구한다.
            int required = challenge.getRequiredWeeklyCount();

            // 오늘을 포함하여 이번 주 남은 일수 계산한다.
            LocalDate weekEndDate = weekStartDate.plusDays(6);  // 일요일을 의미한다.
            long remainingDays = ChronoUnit.DAYS.between(today, weekEndDate) + 1;  // 오늘 기준으로 며칠 남았는지 확인한다.

            // 오늘 안 했고, 실패에 임박하면 경고한다. 오늘 이미 했다면 실패 위험 경고를 줄 이유가 없다.
            boolean isWarning = false;
            if (!isDoIt) {
                // 남은 날을 모두 성공한다고 가정했을 때 이번 주에 달성 가능한 최대 성공 횟수이다.
                int maxPossibleSuccess = successCount + (int) remainingDays;

                if (maxPossibleSuccess == required) {  // 오늘 하지 않으면 실패한다는 경고 메세지를 생성한다.
                    isWarning = true;
                }
            }

            // 유저가 챌린지를 시작한 경과일을 계산한다.
            LocalDate joinedDate = challengeParticipant.getJoinedAt().toLocalDate();  // 참여일에서 시간을 제거한다.
            long elapsedDays = ChronoUnit.DAYS.between(joinedDate, today) + 1;  // 시작일로부터 며칠 지났는지 일수로 계산한다.

            // 챌린지에 관한 정보를 종합하여 List에 넣는다.
            myChallenges.add(MyChallenge.builder()
                    .title(challenge.getTitle())
                    .content(challenge.getContent())
                    .maxParticipantNumber(challenge.getMaxParticipantNumber())
                    .participantNumber(challenge.getParticipantNumber())
                    .duringDate(elapsedDays)
                    .doIt(isDoIt)
                    .warning(isWarning)
                    .build()
            );
        }

        // 얻은 정보들을 dto로 반환한다.
        return new MainHomeResponse(myChallenges, notificationNumber);
    }
}