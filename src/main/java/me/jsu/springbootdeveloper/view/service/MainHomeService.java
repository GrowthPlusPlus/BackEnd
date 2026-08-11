// 정성우

package me.jsu.springbootdeveloper.view.service;

import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeDailyLog;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.challenge.service.ChallengeUtils;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.view.domain.DayStatus;
import me.jsu.springbootdeveloper.view.dto.WeekDayStatusResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.view.dto.MainHomeResponse;
import me.jsu.springbootdeveloper.view.dto.MyChallenge;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/*
    메인화면 구성을 위한 정보를 보내주기 위해 정보들을 정제해주는 서비스 클래스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // 해당 서비스는 조회 전용이다.
public class MainHomeService {
    private final UserService userService;
    private final NotificationService notificationService;
    private final ChallengeQueryService challengeQueryService;

    public MainHomeResponse getMainHome(String userEmail, LocalDate targetDate) {
        User user = userService.findByEmail(userEmail);
        List<MyChallenge> myChallenges = new ArrayList<>();

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));  // 해당 요청이 처리되는 기준이 되는 날짜를 생성한다.
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);  // 주간 상태와 warning 계산에 사용되는 현재 주의 시작일값이다.

        // 유저 id로 읽지 않은 알람 개수를 가져온다.
        long notificationNumber = notificationService.countByUser_IdAndIsReadFalse(user.getId());

        // 선택한 날짜 기준으로 상단 캘린더를 생성한다.
        List<WeekDayStatusResponse> weekStatus = getWeekStatus(user.getId(), today, targetDate);

        // 내가 참여한 챌린지 중, 현재 진행중인 챌린지 목록을 가져온다. 시점은 상관없다.
        List<ChallengeParticipant> joinedChallenges = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(user.getId(), Status.IN_PROGRESS);

        if (joinedChallenges.isEmpty()) {  // 아직 아무런 챌린지에도 참여하지 않는 경우 빈 리스트를 반환한다.
                return new MainHomeResponse(Collections.emptyList(), notificationNumber, weekStatus);
        }

        /*
            오늘 날짜의 하루 데일리 로그를 검색한다. 하나도 수행하지 않았다면 빈 리스트를 반환한다.
            해당 로그 값으로 이번주 총 성공 횟수와 비교하여 챌린지 실패 여부 판단에 사용한다.
         */
        List<ChallengeDailyLog> challengeDailyLogs = challengeQueryService.findByUser_IdAndPostDate(user.getId(), today);
        // 데일리 로그 list를 map으로 바꾸어서 챌린지 id와 데일리 로그의 챌린지 id를 바로 매핑한다.
        Map<Long, ChallengeDailyLog> dailyLogMap = challengeDailyLogs.stream().collect(Collectors.toMap(
                log -> log.getChallenge().getId(),  // 챌린지 id를 map key로 설정한다.
                log -> log,
                (existing, replacement) -> existing  // challengeId 키값 중복 시, 기존 값으로 사용한다.
        ));

        // 유저의 서버 기준 이번주 챌린지 상태를 검색한다. 이번주에 한 번도 수행하지 않았다면 빈 리스트를 반환한다.
        List<ChallengeWeeklyStatus> challengeWeeklyStatuses = challengeQueryService.findByUser_IdAndWeekStartDate(user.getId(), weekStartDate);
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


            // 오늘 인증을 했다면 로그가 있으므로 true, 인증을 하지 않았다면 로그가 존재하지 않으므로 false이다.
            boolean isDoIt = (dailyLog != null);
            boolean isWarning = false;
            /*
                이번 주 챌린지가 실패에 임박했는지 판단한다.
                또한 오늘 안 했고, 실패에 임박하면 경고한다. 오늘 이미 했다면 실패 위험 경고를 줄 이유가 없다.

                즉 오늘 챌린지를 안했다면 if문에 진입한다.
             */
            if (!isDoIt) {
                ChallengeWeeklyStatus weeklyStatus = weeklyStatusMap.get(challengeId);

                // 이번주 성공 횟수를 검색한다.
                int successCount;
                if (weeklyStatus != null) {
                    successCount =  weeklyStatus.getSuccessCount();  // 해당 주에 성공한 횟수
                } else {
                    successCount = 0;
                }

                // 부분 주를 계산한다.
                int adjustedRequiredWeeklyCount = ChallengeUtils.calculateAdjustedRequired(challenge,
                        weekStartDate, weekStartDate.plusDays(6), challengeParticipant.getJoinDate());

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

            // 남은 챌린지 수행 일수를 계산한다.
            LocalDate endDate = challengeParticipant.getChallenge().getEndDate();  // 종료일을 구한다.
            long dDay = ChallengeUtils.calculateDDay(today, endDate);

            // 챌린지에 관한 정보를 종합하여 List에 넣는다.
            myChallenges.add(MyChallenge.builder()
                    .challengeId(challengeId)
                    .title(challenge.getTitle())
                    .requiredWeeklyCount(challenge.getRequiredWeeklyCount())
                    .dueToDate(dDay)
                    .currentStreak(challengeParticipant.getCurrentStreakDays())
                    .todaySuccessCount(challenge.getTodaySuccessCount())
                    .participantNumber(challenge.getParticipantNumber())
                    .doIt(isDoIt)
                    .warning(isWarning)
                    .build()
            );
        }

        // 얻은 정보들을 dto로 반환한다.
        return new MainHomeResponse(myChallenges, notificationNumber, weekStatus);
    }

    /*
        사용자가 선택한 날짜가 속한 주간 캘린더의 색상 정보를 생성한다.
        targetDate를 기준으로 해당 날짜가 속한 월요일 ~ 일요일의 성공, 실패 여부를 조회하여 각 날짜의 상태를 반환한다.
     */
    private List<WeekDayStatusResponse> getWeekStatus(Long userId, LocalDate today, LocalDate targetDate) {
        // 사용자가 선택한 날짜가 속한 주의 월요일을 구한다. 주간 캘린더는 월요일 ~ 일요일 기준으로 구성한다.
        LocalDate targetWeekStart = targetDate.with(DayOfWeek.MONDAY);

        // 해당 주의 마지막 날인 일요일을 구한다.
        LocalDate targetWeekEnd = targetWeekStart.plusDays(6);

        // 해당 주의 성공 로그를 전부 조회한다.
        List<ChallengeDailyLog> logs = challengeQueryService.findByUserIdAndPostDateBetween(
                userId,
                targetWeekStart,
                targetWeekEnd
        );

        /*
            조회한 성공 로그에서 인증 날짜(postDate)만 추출한다.
            Set으로 변환하여 같은 날짜에 여러 챌린지를 성공한 경우에도 해당 날짜 하나만 성공 날짜로 관리한다.
         */
        Set<LocalDate> successDates = logs.stream()
                .map(ChallengeDailyLog::getPostDate)
                .collect(Collectors.toSet());

        // 유저의 모든 실패한 챌린지를 검색한다.
        List<ChallengeParticipant> failedChallenges = challengeQueryService.findStatusWithChallengeByUserIdAndStatus(userId, Status.FAIL);

        // 실패 날짜만 추출한다. 해당 주에 실패하지 않았으면 제외한다.
        Set<LocalDate> failDates = failedChallenges.stream()
                .map(ChallengeParticipant::getFailedAt)
                .filter(Objects::nonNull)
                .filter(date -> !date.isBefore(targetWeekStart) && !date.isAfter(targetWeekEnd))
                .collect(Collectors.toSet());

        // 월요일 ~ 일요일까지 7일의 상태를 저장할 리스트이다.
        List<WeekDayStatusResponse> weekStatus = new ArrayList<>();

        /*
            월요일부터 일요일까지 각 날짜의 상태를 계산한다.
            월 ~ 일 7일에 대해 RED > GREEN > GRAY 우선순위로 계산한다.
         */
        for (int i = 0; i < 7; i++) {
            // 주의 시작일인 월요일에서 i일을 더하여 현재 확인할 날짜를 구한다.
            LocalDate date = targetWeekStart.plusDays(i);

            DayStatus status;

            if (date.isAfter(today)) {  // 서버의 현재 시간인 오늘보다 내일이라면 미래이므로 NONE값을 넣는다.
                status = DayStatus.NONE;
            } else if (failDates.contains(date)) {
                status = DayStatus.RED;
            } else if (successDates.contains(date)) {
                status = DayStatus.GREEN;
            } else {
                status = DayStatus.GRAY;
            }

            // 계산한 날짜와 상태를 주간 캘린더 목록에 추가한다.
            weekStatus.add(WeekDayStatusResponse.builder()
                    .date(date)
                    .status(status)
                    .build());
        }

        // 월요일 ~ 일요일까지 계산한 주간 캘린더 상태를 반환한다.
        return weekStatus;
    }
}