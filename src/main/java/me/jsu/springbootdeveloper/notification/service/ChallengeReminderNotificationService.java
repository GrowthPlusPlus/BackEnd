// 정성우

package me.jsu.springbootdeveloper.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeWeeklyStatus;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeWeeklyStatusRepository;
import me.jsu.springbootdeveloper.fcm.dto.FcmMessageDto;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.user.domain.User;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/*  
    챌린지 주간 인증 마감 60분 전에, 아직 주간 인증 조건(주당 최소 인증 횟수)을
    모두 채우지 못한 사용자에게 푸시 알림(리마인더)을 보내는 서비스이다.
    - 사용자가 참여 중(IN_PROGRESS)인 챌린지만 대상으로 한다.
    - 이번 주의 인증 횟수는 ChallengeWeeklyStatus 테이블의 successCount를 기준으로 판단한다.
    - 전체 푸시 알림이 비활성화된 사용자(all_push_notification_enabled=false)는 제외한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChallengeReminderNotificationService {

    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeWeeklyStatusRepository challengeWeeklyStatusRepository;
    private final NotificationService notificationService;
    private final FcmService fcmService;
    
    /*
        이번 주(월요일 기준 시작) 마감 60분 전에,
        아직 주간 인증 횟수를 모두 채우지 못한 유저들에게
        챌린지 리마인더 알림을 배치로 발송하는 함수이다.

        @param today 기준이 되는 "오늘" 날짜 (Asia/Seoul 기준)
     */
    @Transactional
    public void sendWeeklyChallengeReminders(LocalDate today) {
        // 1. 한국(Asia/Seoul) 시간대에서의 오늘 날짜를 한 번 더 방어적으로 계산한다.
        LocalDate koreaToday = LocalDate.now(ZoneId.of("Asia/Seoul"));

        // 2. 외부에서 today를 주입받았더라도, 서버 기준 날짜와 다르면 방어적으로 서버 날짜를 사용한다.
        if (!koreaToday.equals(today)) {
            today = koreaToday;
        }

        // 3. 이번 주 시작일(월요일)을 계산한다. (ISO 주 기준: 월요일 시작)
        LocalDate weekStartDate = today.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );

        // 4. 현재 진행 중인 챌린지 참가자들을 배치 단위로 조회하기 위한 설정이다.
        Long lastId = 0L;              // 커서 기반 조회를 위한 시작 id
        int batchSize = 500;           // 한 번에 조회할 최대 개수
        Pageable pageable = PageRequest.of(0, batchSize);

        int reminderCount = 0;         // 실제로 알림을 보낸 개수를 집계하기 위한 변수

        while (true) {
            // 5. 진행 중인 챌린지 참가자들을 배치로 불러온다. (챌린지, 유저 fetch join 포함)
            List<ChallengeParticipant> participants =
                    challengeParticipantRepository.findAllInProgressAfterId(lastId, pageable);

            // 6. 더 이상 조회할 데이터가 없으면 반복을 종료한다.
            if (participants.isEmpty()) {
                break;
            }

            // 7. 이번 배치에서 조회한 참가자들 중, 조건에 맞는 유저에게 리마인더를 발송한다.
            for (ChallengeParticipant participant : participants) {
                lastId = participant.getId();  // 다음 배치를 위해 lastId를 갱신한다.

                try {
                    // 7-1. 각 참가자에 대해 리마인더 발송 여부를 판단하고, 필요 시 알림을 보낸다.
                    boolean sent = sendReminderIfNeeded(participant, weekStartDate);
                    if (sent) {
                        reminderCount++;
                    }
                } catch (Exception e) {
                    // 한 참가자 처리 중 예외가 발생해도 나머지 참가자 처리에는 영향을 주지 않도록 한다.
                    log.error("챌린지 리마인더 발송 중 오류 발생 (participantId={}): {}",
                            participant.getId(), e.getMessage(), e);
                }
            }
        }

        // 8. 배치가 끝난 후, 이번 실행에서 총 몇 건의 리마인더를 발송했는지 로그로 남긴다.
        log.info("주간 챌린지 리마인더 배치 완료 - today={}, reminders={}",
                today, reminderCount);
    }

    /*
        단일 챌린지 참가자에 대해, 이번 주 인증 조건을 아직 충족하지 못한 경우에만
        인앱 알림 + 푸시 알림을 발송하는 함수이다.

        @param participant   챌린지 참가자 엔티티
        @param weekStartDate 이번 주 시작일(월요일)
        @return 알림이 실제로 발송되었으면 true, 그렇지 않으면 false
     */
    private boolean sendReminderIfNeeded(ChallengeParticipant participant, LocalDate weekStartDate) {
        // 1. 참가자 및 챌린지, 유저 정보를 가져온다.
        Challenge challenge = participant.getChallenge();
        User user = participant.getUser();

        // 2. 전체 푸시 알림이 비활성화된 유저는 리마인더 대상에서 제외한다.
        if (!user.isAllPushNotificationEnabled()) {
            return false;
        }

        // 3. 현재 날짜(today)가 챌린지 기간 안에 있는지 확인한다.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (today.isBefore(challenge.getStartDate()) || today.isAfter(challenge.getEndDate())) {
            // 아직 시작 전이거나 이미 종료된 챌린지는 리마인더를 보내지 않는다.
            return false;
        }

        // 4. 이번 주에 해당 유저가 이 챌린지에서 몇 번 인증했는지(성공 횟수)를 조회한다.
        Optional<ChallengeWeeklyStatus> weeklyStatusOptional =
                challengeWeeklyStatusRepository.findByUser_IdAndChallenge_IdAndWeekStartDate(
                        user.getId(),
                        challenge.getId(),
                        weekStartDate
                );

        int weeklySuccessCount = weeklyStatusOptional
                .map(ChallengeWeeklyStatus::getSuccessCount)
                .orElse(0);  // 이번 주에 한 번도 인증하지 않았다면 0으로 간주한다.

        // 5. 챌린지에서 요구하는 주당 최소 인증 횟수를 가져온다.
        int requiredWeeklyCount = challenge.getRequiredWeeklyCount();

        // 6. 이미 이번 주 인증 조건을 모두 충족한 경우 리마인더를 보낼 필요가 없다.
        if (weeklySuccessCount >= requiredWeeklyCount) {
            return false;
        }

        // 7. FCM 토큰이 없는 사용자는 푸시 알림을 보낼 수 없으므로 스킵한다.
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
            return false;
        }

        // 8. 푸시알림 메시지 내용을 구성한다.
        String message = String.format(
                "이번 주 \"%s\" 챌린지 인증을 아직 완료하지 않았어요. 마감까지 1시간 남았어요!",
                challenge.getTitle()
        );


        // 9. 푸시 알림(Firebase Cloud Messaging)을 발송한다. 인앱알림엔 해당사항이 아직 없으므로 인앱알림은 보내지 않는다.
        String title = "챌린지 인증 리마인더";
        FcmMessageDto fcmMessageDto = new FcmMessageDto(title, message);
        boolean sent = fcmService.sendMessageTo(user.getFcmToken(), fcmMessageDto);

        if (sent) {
            log.info("사용자 {}에게 챌린지 리마인더 푸시 알림 발송 성공 (challengeId={})",
                    user.getEmail(), challenge.getId());
        } else {
            log.warn("사용자 {}에게 챌린지 리마인더 푸시 알림 발송 실패 (challengeId={})",
                    user.getEmail(), challenge.getId());
        }

        return sent;
    }
}

