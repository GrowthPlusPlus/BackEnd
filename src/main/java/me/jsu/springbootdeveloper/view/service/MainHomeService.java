// 정성우

package me.jsu.springbootdeveloper.view.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeUtils;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.view.dto.MainHomeResponse;
import me.jsu.springbootdeveloper.view.dto.MyChallenge;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.user.service.UserService;
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
    private final ChallengeQueryService challengeQueryService;

    public MainHomeResponse getMainHome(String userEmail, LocalDate today, LocalDate weekStartDate, LocalDate targetDate) {
        User user = userService.findByEmail(userEmail);
        List<MyChallenge> myChallenges = new ArrayList<>();  // 유저의 챌린지 별 정보가 들어갈 변수이다.

        // 유저 id로 읽지 않은 알람 개수를 가져온다.
        long notificationNumber = notificationService.countByUser_IdAndIsReadFalse(user.getId());

        // 요청받은 날짜가 오늘인지, 과거인지, 미래인지 판단한다.
        boolean isToday = targetDate.isEqual(today);
        boolean isPast = targetDate.isBefore(today);  // 현재는 사용하지 않는다.
        boolean isFuture = targetDate.isAfter(today);

        // 과거, 현재, 미래와 같이 시점에 따라 쓰는 경우도 있고 아닌 경우도 있으므로 일단 빈 map으로 초기화 한다.
        Map<Long, ChallengeDailyLog> dailyLogMap = Collections.emptyMap();
        Map<Long, ChallengeWeeklyStatus> weeklyStatusMap = Collections.emptyMap();

        // 내가 참여한 챌린지 중, 현재 진행중인 챌린지 목록을 가져온다. 시점은 상관없다.
        List<ChallengeParticipant> joinedChallenges = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(user.getId(), Status.IN_PROGRESS);
        if (joinedChallenges.isEmpty()) {  // 아직 아무런 챌린지에도 참여하지 않는 경우 빈 리스트를 반환한다.
                return new MainHomeResponse(Collections.emptyList(), notificationNumber);
        }

        /*
            유저가 요청한 날짜의 하루 데일리 로그를 검색한다. 하나도 수행하지 않았다면 빈 리스트를 반환한다.
            과거, 현재의 경우에만 데일리 로그를 검색한다.
         */
        if (!isFuture) {
            List<ChallengeDailyLog> challengeDailyLogs = challengeQueryService.findByUser_IdAndPostDate(user.getId(), targetDate);
            // 데일리 로그 list를 map으로 바꾸어서 챌린지 id와 데일리 로그의 챌린지 id를 바로 매핑한다.
            dailyLogMap = challengeDailyLogs.stream().collect(Collectors.toMap(
                    log -> log.getChallenge().getId(),  // 챌린지 id를 map key로 설정한다.
                    log -> log
            ));
        }

        /*
            유저의 이번주 챌린지 상태를 검색한다. 이번주에 한 번도 수행하지 않았다면 빈 리스트를 반환한다.
            오늘만 주간 상태가 의미가 있으므로, 현재가 아니면 처리하지 않는다.
         */
        if (isToday) {
            List<ChallengeWeeklyStatus> challengeWeeklyStatuses = challengeQueryService.findByUser_IdAndWeekStartDate(
                    user.getId(), weekStartDate
            );
            // 위와 마찬가지로 챌린지 id를 매핑하기 위해 map을 사용한다.
            weeklyStatusMap = challengeWeeklyStatuses.stream().collect(Collectors.toMap(
                    ws -> ws.getChallenge().getId(),
                    ws -> ws
            ));
        }

        for (ChallengeParticipant challengeParticipant : joinedChallenges) {
            Challenge challenge = challengeParticipant.getChallenge();  // 챌린지 하나를 가져온다.
            Long challengeId = challenge.getId();

            // 오늘 데일리 로그 여부를 판별한다.
            ChallengeDailyLog dailyLog = dailyLogMap.get(challengeId);

            /*
                미래엔 dailyLog가 비어있으므로 항상 false값을 가진다.
                하지만 안했다는 의미가 아닌 판단이 불가하다는 의미로, 어떻게 사용할지는 프론트가 판단한다.
                오늘 인증을 했다면 로그가 있으므로 true, 인증을 하지 않았다면 로그가 존재하지 않으므로 false이다.
             */
            boolean isDoIt = (dailyLog != null);

            boolean isWarning = false;
            /*
                이번 주 챌린지가 실패에 임박했는지 판단한다. 오늘만 판단하고 과거나 미래는 판단하지 않는다.
                또한 오늘 안 했고, 실패에 임박하면 경고한다. 오늘 이미 했다면 실패 위험 경고를 줄 이유가 없다.

                즉 요청받은 날짜가 오늘이며, 오늘 챌린지를 안했다면 if문에 진입한다.
             */
            if (isToday && !isDoIt) {
                ChallengeWeeklyStatus weeklyStatus = weeklyStatusMap.get(challengeId);

                // 이번주 성공 횟수를 검색한다.
                int successCount;
                if (weeklyStatus != null) {
                    successCount =  weeklyStatus.getSuccessCount();  // 해당 주에 성공한 횟수
                } else {
                    successCount = 0;
                }

                // 부분 주를 계산한다.
                int adjustedRequiredWeeklyCount = ChallengeUtils.calculateAdjustedRequired(challenge, weekStartDate,weekStartDate.plusDays(6));

                /*
                    effectiveStart와 effectiveEnd는 남은 가능 일수를 계산하기 위해 필요한 값이다.
                    effectiveStart는 12시가 넘어간 시점이므로 내일부터 챌린지를 수행 가능한 날짜에 포함한다.
                 */
                LocalDate effectiveStart;
                if (today.plusDays(1).isAfter(challenge.getStartDate())) {  // 내일이 챌린지 시작 이후인지 검사한다.
                    effectiveStart = today.plusDays(1);  // 보통은 내일부터 수행 가능한 날로 정한다.
                }
                else {
                    effectiveStart = challenge.getStartDate();  // 방어코드이다.
                }

                // 챌린지 종료일과 이번주 종료일 중, 더 이른 날짜를 수행 가능한 날짜로 잡는다. 주가 더 남았어도 챌린지가 종료되면 수행이 불가능하기 때문이다.
                LocalDate effectiveEnd;
                if (challenge.getEndDate().isBefore(weekStartDate.plusDays(6))) {  // 챌린지 종료가 주 마지막 날보다 더 빠른 경우이다.
                    effectiveEnd = challenge.getEndDate();
                }
                else {  // 일반적인 케이스이다.
                    effectiveEnd = weekStartDate.plusDays(6);
                }

                long remainingDays = 0;  // 앞으로 성공 횟수를 추가로 올릴 수 있는 최대 횟수의 상한이다.
                if (!effectiveStart.isAfter(effectiveEnd)) {  // 시작일이 마지막 수행일보다 뒤가 아닌 경우이다. 즉 수행 가능한 날짜가 남았다.
                    // 날짜 범위는 양 끝을 포함하므로 +1을 해야 한다. ex) start: 8/11, end: 8/11
                    remainingDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
                }

                isWarning = (successCount + remainingDays + 1) == adjustedRequiredWeeklyCount;
            }

            // 유저가 챌린지를 시작한 경과일을 계산한다.
            LocalDate joinedDate = challengeParticipant.getCreatedAt().toLocalDate();  // 참여일에서 시간을 제거한다.
            long elapsedDaysRaw = ChronoUnit.DAYS.between(joinedDate, targetDate) + 1;  // 시작일로부터 며칠 지났는지 일수로 계산한다.
            long elapsedDays = Math.max(0, elapsedDaysRaw);  // 경과일이 음수가 나오는 것을 방지한다.

            // 챌린지에 관한 정보를 종합하여 List에 넣는다.
            myChallenges.add(MyChallenge.builder()
                    .challengeId(challengeId)
                    .title(challenge.getTitle())
                    .content(challenge.getContent())
                    .todaySuccessCount(challenge.getTodaySuccessCount())
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