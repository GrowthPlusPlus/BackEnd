// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.enumType.Status;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeWeeklyStatusRepository;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.notification.service.NotificationService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.service.UserStatisticsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.util.Pair;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChallengeMaintenanceService {
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;
    private final ChallengeDeleteService challengeDeleteService;
    private final UserStatisticsService userStatisticsService;
    private final NotificationService notificationService;
    private final UserProfilePictureRepository userProfilePictureRepository;
    private final FcmService fcmService;

    private static final int BATCH_SIZE = 500;  // 해당 배치 사이즈로 처리한다.

    // 오늘 챌린지를 몇 명이 수행했는지 알아내기 위해 날짜를 보정한다.
    public void syncToday(LocalDate today) {
        Long lastId = 0L;
        List<Challenge> batch;

        do {
            // 현재 진행중인 챌린지들을 검색한다.
            batch = challengeRepository.findActiveChallengesAfterId(today, lastId, PageRequest.of(0, BATCH_SIZE));

            for (Challenge challenge : batch) {
                challenge.syncToday(today);  // 무조건 오늘로 갱신한다.
                lastId = challenge.getId();  // 커서를 이동한다.
            }
        } while (!batch.isEmpty());
    }

    /*
        하루가 지났을 때, 유저가 어제 챌린지를 수행하지 않았다면 연속 일수를 초기화 하는 함수이다.
     */
    public void resetStreakIfBroken(LocalDate today) {
        // 배치를 처리하기 위해 커서 기반으로 검색한다.
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        do {
            // 진행중인 챌린지들을 배치 사이즈로 검색한다.
            batch = challengeParticipantRepository.findAllInProgressAfterId(lastId, PageRequest.of(0, BATCH_SIZE));
            for (ChallengeParticipant cp : batch) {
                // 연속 수행 이력이 없는 경우이다.
                if (cp.getCurrentStreakEndDate() == null) {
                    cp.initCurrentStreakDays();

                    lastId = cp.getId();
                    continue;  // NPE를 방지한다.
                }

                LocalDate yesterday = today.minusDays(1);  // 어제 날짜
                if (!cp.getCurrentStreakEndDate().isEqual(yesterday)) {  // 마지막 성공 날짜가 어제가 아니면 연속 수행이 끊긴 것이므로 초기화 한다.
                    cp.initCurrentStreakDays();
                }

                lastId = cp.getId();  // 커서를 이동시킨다.
            }
        } while (!batch.isEmpty());  // 조회할 데이터가 없을 때까지 반복한다.
    }

    /*
        지난 주차를 "확정" 실패 판정하는 함수이다.
        requireWeeklyCount가 1일 때도, 지난 주가 완전히 끝난 뒤 확정 실패를 처리하기 위해 분리한 1차 루프이다.
        한 번에 모든 값을 가져오면 부하가 크므로 batch 사이즈 단위로 처리한다.
     */
    public void finalizePreviousWeekFailures(LocalDate today) {
        // 배치를 처리하기 위해 커서 기반으로 검색한다.
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        // 지난 주의 범위를 계산한다. (월요일 ~ 일요일)
        LocalDate previousWeekStart = today.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousWeekEnd = previousWeekStart.plusDays(6);

        while (true) {
            // 진행중인 챌린지들을 배치 사이즈로 검색한다.
            batch = challengeParticipantRepository.findAllInProgressAfterId(lastId, PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) {  // 조회된 데이터가 없다면 탈출한다.
                break;
            }

            // userId를 모두 수집한다.
            List<Long> userIds = batch.stream()
                    .map(cp -> cp.getUser().getId())
                    .distinct()
                    .toList();

            // challengeId를 모두 수집한다.
            List<Long> challengeIds = batch.stream()
                    .map(cp -> cp.getChallenge().getId())
                    .distinct()
                    .toList();

            // 지난 주 기준 WeeklyStatus를 한 번에 조회한다. 지난 주에 한 번도 수행하지 않은 유저는 값이 없다.
            List<ChallengeWeeklyStatus> weeklyStatuses = challengeWeeklyStatusRepository.findAllByUsersAndChallenges(userIds, challengeIds, previousWeekStart);

            // 모든 유저의 (userId, challengeId)를 pair 키로 만들어 WeeklyStatus로 매핑한다.
            Map<Pair<Long, Long>, ChallengeWeeklyStatus> weeklyMap =
                    weeklyStatuses.stream()
                            .collect(Collectors.toMap(
                                    ws -> Pair.of(ws.getUser().getId(), ws.getChallenge().getId()),
                                    ws -> ws,
                                    (a, b) -> a  // 중복 방지를 위한 방어 코드이다.
                            ));

            for (ChallengeParticipant cp : batch) {  // 하나씩 검사한다.
                Pair<Long, Long> key = Pair.of(cp.getUser().getId(), cp.getChallenge().getId());  // 유저 정보를 토대로 키를 생성한다.
                ChallengeWeeklyStatus weeklyStatus = weeklyMap.get(key);  // 주간 상태가 없으면 null이다.

                checkPreviousWeekFailure(cp, weeklyStatus, today, previousWeekStart, previousWeekEnd);  // 지난 주 "확정" 실패 판정을 수행한다.
                lastId = cp.getId();  // 커서를 이동시킨다.
            }
        }
    }

    /*
        챌린지의 주간 상태를 판단하는 함수이다. 해당 주에 실패하면 챌린지 자체를 실패한다.
        한 번에 모든 값을 가져오는 것은 부하가 크므로 batch사이즈로 나누어 계산한다.
        이 함수는 "이번 주 조기 실패(2차 루프)"만 담당한다.
     */
    public void failWeeklyStatuses(LocalDate today) {
        // 배치를 처리하기 위해 커서 기반으로 검색한다.
        Long lastId = 0L;
        List<ChallengeParticipant> batch;

        while (true) {
            // 진행중인 챌린지들을 배치 사이즈로 검색한다.
            batch = challengeParticipantRepository.findAllInProgressAfterId(lastId, PageRequest.of(0, BATCH_SIZE));
            if (batch.isEmpty()) {  // 조회된 데이터가 없다면 탈출한다.
                break;
            }

            // userId를 모두 수집한다.
            List<Long> userIds = batch.stream()
                    .map(cp -> cp.getUser().getId())
                    .distinct()
                    .toList();

            // challengeId를 모두 수집한다.
            List<Long> challengeIds = batch.stream()
                    .map(cp -> cp.getChallenge().getId())
                    .distinct()
                    .toList();

            // 해당 주간 상태의 유효 범위를 구한다.
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));  // 날짜를 기준으로 해당 주를 구한다.
            LocalDate weekEnd = weekStart.plusDays(6);

            // 유저 id, 챌린지 id, 해당 주로 WeeklyStatus를 한 번에 조회한다. 주에 한 번도 챌린지를 수행하지 않은 유저는 값이 없다.
            List<ChallengeWeeklyStatus> weeklyStatuses = challengeWeeklyStatusRepository.findAllByUsersAndChallenges(userIds, challengeIds, weekStart);

            // 모든 유저의 (userId, challengeId)를 pair 키로 만들어 WeeklyStatus로 매핑한다.
            Map<Pair<Long, Long>, ChallengeWeeklyStatus> weeklyMap =
                    weeklyStatuses.stream()
                            .collect(Collectors.toMap(
                                    ws -> Pair.of(ws.getUser().getId(), ws.getChallenge().getId()),
                                    ws -> ws,
                                    (a, b) -> a  // 중복 방지를 위한 방어 코드이다.
                            ));

            for (ChallengeParticipant cp : batch) {  // 하나씩 검사한다.
                Pair<Long, Long> key = Pair.of(cp.getUser().getId(), cp.getChallenge().getId());  // 유저 정보를 토대로 키를 생성한다.
                ChallengeWeeklyStatus weeklyStatus = weeklyMap.get(key);  // 키를 아까 매핑한 map에 사용하여 반복적인 쿼리문 없이 주간 상태를 얻는다. 없으면 null이다.

                checkWeeklyFailure(cp, weeklyStatus, today, weekStart, weekEnd);  // 하나씩 실패 판정을 수행한다.
                lastId = cp.getId();  // 커서를 이동시킨다.
            }
        }
    }

    /*
        지난 주차를 "확정" 실패 판정한다.
        checkWeeklyFailure와 달리 남은 일수(remainingDays) 개념을 쓰지 않는다.
        주차가 이미 종료되었으므로, 지난 주 실적(successCount)과 요구치(adjustedRequired)만 비교한다.
     */
    private void checkPreviousWeekFailure(ChallengeParticipant cp, ChallengeWeeklyStatus weeklyStatus, LocalDate today, LocalDate weekStart, LocalDate weekEnd) {
        Challenge challenge = cp.getChallenge();

        // 챌린지와 주차가 아예 겹치지 않으면 판정 대상이 아니다.
        if (challenge.getEndDate().isBefore(weekStart) || challenge.getStartDate().isAfter(weekEnd)) {
            return;
        }

        int successCount;
        if (weeklyStatus != null) {
            successCount = weeklyStatus.getSuccessCount();  // 지난 주에 성공한 횟수
        }
        else {  // 지난 주에 한 번도 수행하지 않았다면 0으로 간주한다.
            successCount = 0;
        }

        // 지난 주 범위에 맞는 부분 주 보정 요구치이다.
        int adjustedRequired = ChallengeUtils.calculateAdjustedRequired(challenge, weekStart, weekEnd);

        // 지난 주 "확정" 실패 판정이다.
        if (successCount < adjustedRequired) {
            if (weeklyStatus != null) {  // 지난 주에 한 번이라도 수행한 경우이다. 조건문이 없다면 NPE 가능성이 있다.
                weeklyStatus.markFail();  // 주간 상태를 실패로 바꾼다.
            }
            cp.markFail(today);  // 챌린지 상태를 실패로 바꾼다.
            userStatisticsService.updateFailedChallengeCount(cp.getUser().getId());  // 실패한 챌린지 개수를 늘린다.

            markParticipantFail(cp);  // 참가자 실패, 통계, 알림을 공통 처리한다.
        }
    }

    /*
        하루가 지났을 때, 챌린지가 실패했는지 하나씩 검사해주는 함수이다.
     */
    private void checkWeeklyFailure(ChallengeParticipant cp, ChallengeWeeklyStatus weeklyStatus, LocalDate today, LocalDate weekStart, LocalDate weekEnd) {
        Challenge challenge = cp.getChallenge();  // 해당 주간 상태가 속한 챌린지이다.

        int successCount;
        if (weeklyStatus != null) {
            successCount =  weeklyStatus.getSuccessCount();  // 해당 주에 성공한 횟수
        }
        else {  // Optional.empty()인 케이스로 해당 주에 한 번도 수행하지 않았으므로 0 처리한다.
            successCount = 0;
        }

        // 부분 주 보정이 적용된 최소 주간 성공 횟수로, 첫 주차나 마지막 주차를 위해 사용한다.
        int adjustedRequired = ChallengeUtils.calculateAdjustedRequired(challenge, weekStart, weekEnd);

        /*
            effectiveStart와 effectiveEnd는 남은 가능 일수를 계산하기 위해 필요한 값이다.
            effectiveStart는 12시가 넘어간 시점이므로 오늘부터 챌린지를 수행 가능한 날짜에 포함한다.
            오늘을 기준으로 effectiveStart가 계산된다.
         */
        LocalDate effectiveStart;
        if (challenge.getStartDate().isAfter(today)) {  // 챌린지 시작일보다 오늘이 더 이전이라면 문제 상황이므로 리턴한다.
            return;
        }
        else {
            effectiveStart = today;  // 모든 경우이다.
        }

        // 챌린지 종료일과 이번주 종료일 중, 더 이른 날짜를 수행 가능한 날짜로 잡는다. 주가 더 남았어도 챌린지가 종료되면 수행이 불가능하기 때문이다.
        LocalDate effectiveEnd;
        if (challenge.getEndDate().isBefore(weekEnd)) {  // 챌린지 종료가 주 마지막 날보다 더 빠른 경우이다.
            effectiveEnd = challenge.getEndDate();
        }
        else {  // 일반적인 케이스이다.
            effectiveEnd = weekEnd;
        }

        long remainingDays = 0;  // 앞으로 성공 횟수를 추가로 올릴 수 있는 최대 횟수의 상한이다.
        if (!effectiveStart.isAfter(effectiveEnd)) {  // 시작일이 마지막 수행일보다 뒤가 아닌 경우이다. 즉 수행 가능한 날짜가 남았다.
            // 날짜 범위는 양 끝을 포함하므로 +1을 해야 한다. ex) start: 8/11, end: 8/11
            remainingDays = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd) + 1;
        }

        // 조기 실패 판정을 결정한다. 요구된 최소 횟수가 지금까지 수행한 횟수 + 남은 수행 가능 횟수보다 적으면 실패한다.
        if (successCount + remainingDays < adjustedRequired) {
            if (weeklyStatus != null) {  // 해당 주에 한 번이라도 수행한 경우이다. 조건문이 없다면 NPE 가능성이 있다.
                weeklyStatus.markFail();  // 주간 상태를 실패로 바꾼다.
            }
            cp.markFail(today);  // 챌린지 상태를 실패로 바꾼다.
            userStatisticsService.updateFailedChallengeCount(cp.getUser().getId());  // 실패한 챌린지 개수를 늘린다.

            markParticipantFail(cp);  // 참가자 실패, 통계, 알림을 공통 처리한다.
        }
    }

    /*
        챌린지 참가자를 실패로 처리하는 공통 함수이다.
        이번 주 조기 실패와 지난 주 확정 실패에서 동일하게 호출한다.
     */
    private void markParticipantFail(ChallengeParticipant cp) {
        Challenge challenge = cp.getChallenge();

        //챌린지 실패 알림을 보낸다
        //아쉬워요! '챌린지명' 달성에 실패했지만, 다음엔 꼭 해낼 수 있어요.
        String title = "챌린지 실패";
        String message = "아쉬워요! '" + challenge.getTitle() + "' 달성에 실패했지만, 다음엔 꼭 해낼 수 있어요.";

        //인앱 알림 발송
        User user = cp.getUser();
        long userId = user.getId();
        Long targetId = challenge.getId();
        String imageUrl = userProfilePictureRepository.findImageUrlByUserId(userId).orElse("");
        notificationService.sendNotification(user, NotificationType.CHALLENGE, message, imageUrl, targetId);

        // 푸시 알림을 발송
        // 챌린지 푸시 알림이 활성화되어 있고, FCM 토큰이 있는 경우에만 발송한다.
        if (user.isAllPushNotificationEnabled()
                && user.getFcmToken() != null
                && !user.getFcmToken().isBlank()) {
            fcmService.sendMessageTo(user.getFcmToken(), title, message);

        }
    }

    /*
        진행중인 챌린지들을 조회하여 방장이 챌린지를 실패했는지 검사하는 함수이다.
        실패한경우 진행중인 참가자에게 방장을 위임한다.
     */
    @Transactional
    public void delegateOwnerIfFailed(LocalDate today) {
        Long lastId = 0L;
        List<Challenge> batch;

        do {
            batch = challengeRepository.findActiveChallengesAfterId(today, lastId, PageRequest.of(0, BATCH_SIZE));

            if (batch.isEmpty()) break;

            // batch에서 challengeId를 수집한다.
            List<Long> challengeIds = batch.stream()
                    .map(Challenge::getId)
                    .toList();

            // batch에서 ownerId를 수집한다.
            List<Long> ownerIds = batch.stream()
                    .map(c -> c.getUser().getId())
                    .toList();

            // 방장 정보, 챌린지를 진행중인 참가자들을 한 번에 조회한다.
            List<ChallengeParticipant> allParticipants = challengeParticipantRepository.findParticipantsWithOwner(challengeIds, ownerIds, Status.IN_PROGRESS);

            // 참가자와 방장을 분리하는 map을 생성한다.
            Map<Long, ChallengeParticipant> ownerMap = new HashMap<>();
            Map<Long, List<ChallengeParticipant>> participantMap = new HashMap<>();

            // 방장 id 검색의 효율을 위해 사용한다.
            Set<Long> ownerIdSet = new HashSet<>(ownerIds);

            // 두 후보군을 분리한다.
            for (ChallengeParticipant cp : allParticipants) {
                Long challengeId = cp.getChallenge().getId();

                // owner인지 판별
                if (ownerIdSet.contains(cp.getUser().getId())) {  // 참가자의 id가 처음 수집한 방장 id와 같다면 map에 추가한다.
                    ownerMap.putIfAbsent(challengeId, cp);
                }
                else {  // 아니라면 참가자로 분리한다.
                    participantMap
                            .computeIfAbsent(challengeId, k -> new ArrayList<>())
                            .add(cp);
                }
            }

            for (Challenge challenge : batch) {
                lastId = challenge.getId();

                ChallengeParticipant ownerParticipant = ownerMap.get(challenge.getId());  // 챌린지 id로 해당 챌린지의 방장 정보를 가져온다.

                // 방장이 실패한 상태가 아니라면 스킵한다.
                if (ownerParticipant == null || ownerParticipant.getStatus() != Status.FAIL) {
                    continue;
                }

                // 방장 후보를 가져온다.
                List<ChallengeParticipant> participants = participantMap.getOrDefault(challenge.getId(), List.of());
                if (participants.isEmpty()) {  // 남은 사람이 없는 경우, 챌린지 상태를 비활성화 시킨다.
                    challenge.failChallenge();

                    continue;
                }

                // 스코어가 높은 유저를 다음 방장으로 선택한다.
                User nextOwner = challengeDeleteService.selectNextOwner(participants);

                // 방장을 위임한다.
                challenge.updateUser(nextOwner);
            }

        } while (!batch.isEmpty());
    }

    /*
        챌린지의 최종 성공을 판정하는 함수이다.
     */
    public void finalizeChallenges(LocalDate today) {
        Long lastId = 0L;
        List<ChallengeParticipant> batch;  // 성능을 위해 배치 사이즈로 나눈다.

        do {
            // 오늘을 기준으로 진행중이면서, 종료일인 챌린지들을 구한다.
            batch = challengeParticipantRepository.findEndedInProgressAfterId(lastId, today, PageRequest.of(0, BATCH_SIZE));

            // 위의 주간 상태 검사를 먼저 통과했으므로, FAIL되지 않은 챌린지들은 모두 성공처리된다.
            for (ChallengeParticipant cp : batch) {
                cp.markSuccess(cp.getChallenge().getEndDate());  // 챌린지 상태를 성공으로 바꾼다.
                userStatisticsService.updateCompletedChallengeCount(cp.getUser().getId());  // 성공한 챌린지 개수를 늘린다.

                //챌린지 성공 알림을 보낸다
                //축하합니다! '챌린지명' 목표를 훌륭하게 해냈어요!
                String title = "챌린지 성공";
                String message = "축하합니다! '" + cp.getChallenge().getTitle() + "' 목표를 훌륭하게 해냈어요!";
                //인앱 알림 발송
                User user = cp.getUser();
                long userId = user.getId();
                Long targetId = cp.getChallenge().getId();
                String imageUrl = "";
                notificationService.sendNotification(user, NotificationType.CHALLENGE, message, imageUrl, targetId);

                // 푸시 알림을 발송
                // 챌린지 푸시 알림이 활성화되어 있고, FCM 토큰이 있는 경우에만 발송한다.
                if (user.isAllPushNotificationEnabled()
                        && user.getFcmToken() != null
                        && !user.getFcmToken().isBlank()) {
                    fcmService.sendMessageTo(user.getFcmToken(), title, message);

                }
                lastId = cp.getId();  // 커서를 이동한다
            }
        } while (!batch.isEmpty());
    }
}
