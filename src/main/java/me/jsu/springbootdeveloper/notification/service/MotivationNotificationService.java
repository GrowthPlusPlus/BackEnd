// 정성우

package me.jsu.springbootdeveloper.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.fcm.dto.FcmMessageDto;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;
import me.jsu.springbootdeveloper.notification.repository.UserNotificationScheduleRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/*
    동기부여 메시지 알림을 관리하는 서비스이다.
    사용자별 알림 시간 설정을 저장하고, 매일 지정된 시간에 동기부여 메시지를 발송한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MotivationNotificationService {
    private final UserNotificationScheduleRepository scheduleRepository;
    private final UserService userService;
    private final FcmService fcmService;
    private final Random random = new Random();

    /*
        동기부여 메시지 풀이다.
        매일 지정된 시간에 이 메시지들 중 하나를 랜덤하게 선택하여 발송한다.
     */
    private static final List<String> MOTIVATION_MESSAGES = List.of(
            "오늘도 화이팅! 작은 걸음이 큰 변화를 만듭니다. 💪",
            "당신의 노력은 결코 헛되지 않습니다. 계속해서 나아가세요! ✨",
            "어제보다 오늘이, 오늘보다 내일이 더 나아질 거예요. 🌟",
            "지금 이 순간도 당신은 성장하고 있습니다. 멈추지 마세요! 🚀",
            "작은 성취도 축하할 가치가 있습니다. 오늘 하루도 수고하셨어요! 🎉",
            "포기하지 않는 한 실패는 없습니다. 당신은 할 수 있어요! 💯",
            "매일 조금씩이라도 나아가는 당신이 멋져요. 계속 화이팅! 🌈",
            "도전하는 용기, 그 자체가 이미 성공입니다. 응원합니다! 🎯",
            "오늘의 작은 노력이 내일의 큰 성과가 됩니다. 화이팅! ⭐",
            "당신의 꿈을 향해 한 걸음씩 나아가는 모습이 아름답습니다. 💫"
    );

    /*
        사용자의 동기부여 알림 시간을 설정하거나 업데이트한다.
        이미 설정이 있으면 업데이트하고, 없으면 새로 생성한다.
        
        @param email 사용자 이메일
        @param notificationTime 알림을 받을 시간 (예: 09:00)
        @param enabled 알림 활성화 여부
     */
    @Transactional
    public void setNotificationSchedule(String email, LocalTime notificationTime, boolean enabled) {
        // 1. 사용자 정보를 조회한다.
        User user = userService.findByEmail(email);

        // 2. 기존 설정이 있는지 확인한다.
        scheduleRepository.findByUser_Id(user.getId())
                .ifPresentOrElse(
                        // 기존 설정이 있으면 업데이트한다.
                        schedule -> {
                            schedule.updateNotificationTime(notificationTime);
                            schedule.setEnabled(enabled);
                            log.info("동기부여 알림 시간 설정을 업데이트했습니다. 사용자: {}, 시간: {}, 활성화: {}",
                                    email, notificationTime, enabled);
                        },
                        // 기존 설정이 없으면 새로 생성한다.
                        () -> {
                            UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                    .user(user)
                                    .notificationTime(notificationTime)
                                    .enabled(enabled)
                                    .build();
                            scheduleRepository.save(newSchedule);
                            log.info("동기부여 알림 시간 설정을 생성했습니다. 사용자: {}, 시간: {}, 활성화: {}",
                                    email, notificationTime, enabled);
                        }
                );
    }

    /*
        사용자의 동기부여 알림 설정을 조회한다.
        
        @param email 사용자 이메일
        @return 알림 시간 설정 (없을 경우 null)
     */
    @Transactional(readOnly = true)
    public UserNotificationSchedule getNotificationSchedule(String email) {
        User user = userService.findByEmail(email);
        return scheduleRepository.findByUser_Id(user.getId()).orElse(null);
    }

    /*
        특정 시간에 알림을 받도록 설정된 모든 활성화된 사용자에게 동기부여 메시지를 발송한다.
        스케줄러에서 호출한다.
        
        @param notificationTime 알림을 발송할 시간
     */
    @Transactional
    public void sendMotivationNotifications(LocalTime notificationTime) {
        // 1. 스케줄러는 '정각(분=0)' 기준으로 호출하지만, 사용자가 설정한 시간은 '시(hour)'만 비교한다.
        int notificationHour = notificationTime.getHour();

        // 2. 활성화된 모든 알림 설정을 조회한 뒤 '시(hour)'만 필터링한다.
        //    - 분(minute)은 무시하므로 설정이 09:15여도 09시에 발송된다.
        List<UserNotificationSchedule> schedules = scheduleRepository.findByEnabledTrue();
        schedules = schedules.stream()
                .filter(schedule -> schedule.getNotificationTime() != null)
                .filter(schedule -> schedule.getNotificationTime().getHour() == notificationHour)
                .toList();

        if (schedules.isEmpty()) {
            log.debug("정각 시(hour) {}에 알림을 받을 사용자가 없습니다.", notificationHour);
            return;
        }

        log.info("정각 시(hour) {}에 {}명의 사용자에게 동기부여 메시지를 발송합니다.", notificationHour, schedules.size());

        // 2. 각 사용자에게 동기부여 메시지를 발송한다.
        for (UserNotificationSchedule schedule : schedules) {
            try {
                sendMotivationNotificationToUser(schedule.getUser());
            } catch (Exception e) {
                // 한 사용자에게 발송 실패해도 다른 사용자들에게는 계속 발송한다.
                log.error("사용자 {}에게 동기부여 메시지 발송 실패: {}",
                        schedule.getUser().getEmail(), e.getMessage(), e);
            }
        }
    }

    /*
        특정 사용자에게 동기부여 메시지를 발송한다.
        랜덤하게 선택된 메시지를 인앱 알림과 푸시 알림으로 발송한다.
        
        @param user 알림을 받을 사용자
     */
    @Transactional
    public void sendMotivationNotificationToUser(User user) {
        // 1. 랜덤하게 동기부여 메시지를 선택한다.
        String message = MOTIVATION_MESSAGES.get(random.nextInt(MOTIVATION_MESSAGES.size()));

        /*3. 인앱 알림을 DB에 저장한다. -> 현재 동기부여 메세지는 푸시알림만 가도록 구현
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(NotificationType.MOTIVATION)
                .build();
        notificationRepository.save(notification);
        */
        // 2. FCM 토큰이 있으면 푸시 알림을 발송한다.
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            String title = "오늘의 동기부여 메시지";
            FcmMessageDto messageDto = new FcmMessageDto(title, message);
            boolean sent = fcmService.sendMessageTo(user.getFcmToken(), messageDto);
            
            if (sent) {
                log.info("사용자 {}에게 동기부여 메시지 푸시 알림 발송 성공", user.getEmail());
            } else {
                log.warn("사용자 {}에게 동기부여 메시지 푸시 알림 발송 실패", user.getEmail());
            }
        } else {
            log.debug("사용자 {}의 FCM 토큰이 없어 푸시 알림을 발송하지 않습니다.", user.getEmail());
        }
    }
}
