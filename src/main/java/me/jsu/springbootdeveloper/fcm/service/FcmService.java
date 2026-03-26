// 정성우

package me.jsu.springbootdeveloper.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.jsu.springbootdeveloper.fcm.dto.FcmMessageDto;
import me.jsu.springbootdeveloper.fcm.dto.PushNotificationSettingsResponse;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;
import me.jsu.springbootdeveloper.notification.repository.UserNotificationScheduleRepository;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/*
    Firebase Cloud Messaging을 통해 푸시 알림을 발송하는 서비스이다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FcmService {
    private static final int MAX_RETRY_ATTEMPTS = 3;  // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000;  // 재시도 간격 (1초)
    
    private final UserRepository userRepository;  // FCM 토큰 등록/업데이트/삭제 및 만료된 토큰 삭제를 위해 사용
    private final ChallengeParticipantRepository challengeParticipantRepository;  // 개별 챌린지 알림 토글 연쇄 변경에 사용한다.
    private final UserNotificationScheduleRepository userNotificationScheduleRepository;  // 스케줄러 기반 알림 설정(동기부여/리마인더 전역 토글)을 관리한다.

    /*
        로그인한 사용자의 "푸시 알림 설정 페이지"에 필요한 값을 조회한다.

        - User 테이블에 있는 토글(전체/친구/멤버 반응/멤버 인증/챌린지 초대)을 포함한다.
        - 스케줄러 기반 토글(동기부여/전체 리마인더)은 UserNotificationSchedule에서 조회한다.
        - UserNotificationSchedule이 없으면, 정책에 따라 기본값으로 생성한다.

        @param email 사용자 이메일
        @return 푸시 알림 설정 조회 DTO
     */
    @Transactional
    public PushNotificationSettingsResponse getPushNotificationSettings(String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    UserNotificationSchedule schedule = userNotificationScheduleRepository
                            .findByUser_Id(user.getId())
                            .orElseGet(() -> {
                                LocalTime defaultTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
                                        .withMinute(0)
                                        .withSecond(0)
                                        .withNano(0);

                                UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                        .user(user)
                                        .notificationTime(defaultTime)
                                        .enabled(false)
                                        .dailyReminderPushNotificationEnabled(true)
                                        .build();
                                return userNotificationScheduleRepository.save(newSchedule);
                            });

                    return PushNotificationSettingsResponse.builder()
                            //전체 알림 활성화 여부 
                            .allPushNotificationEnabled(user.isAllPushNotificationEnabled())
                            //친구 관련 푸시 알림 활성화 여부
                            .friendPushNotificationEnabled(user.isFriendPushNotificationEnabled())
                            //전체 멤버 반응 푸시 알림 활성화 여부
                            .allMemberReactionPushNotificationEnabled(user.isAllMemberReactionPushNotificationEnabled())
                            //전체 멤버 인증 푸시 알림 활성화 여부
                            .allMemberCertificationPushNotificationEnabled(user.isAllMemberCertificationPushNotificationEnabled())
                            //챌린지 초대 푸시 알림 활성화 여부
                            .challengeInvitePushNotificationEnabled(user.isChallengeInvitePushNotificationEnabled())
                            //동기부여 메시지 푸시 알림 활성화 여부
                            .motivationPushNotificationEnabled(schedule.isEnabled())
                            //(전체) 일일 리마인더 푸시 알림 활성화 여부
                            .dailyReminderPushNotificationEnabled(schedule.isDailyReminderPushNotificationEnabled())
                            .build();
                })
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
    }
    /*
        특정 사용자의 FCM 토큰으로 푸시 알림을 발송한다.
        푸시 알림 발송 실패 시에도 예외를 던지지 않고 로그만 남긴다.
        이는 푸시 알림 실패가 메인 비즈니스 로직(친구 신청 수락 등)에 영향을 주지 않도록 하기 위함이다.
        일시적 오류 발생 시 자동으로 재시도한다.
        
        @param fcmToken: 수신자의 FCM 토큰
        @param messageDto: 푸시 알림 제목과 내용을 담은 DTO
        @return: 푸시 알림 발송 성공 여부
     */
    public boolean sendMessageTo(String fcmToken, FcmMessageDto messageDto) {
        // FCM 토큰이 없으면 알림을 보낼 수 없다.
        if (fcmToken == null || fcmToken.isBlank()) {
            log.debug("FCM 토큰이 없어 푸시 알림을 발송할 수 없습니다.");
            return false;
        }

        // 재시도 로직을 포함하여 푸시 알림을 발송한다.
        return sendMessageWithRetry(fcmToken, messageDto, 0);
    }

    /*
        재시도 로직을 포함하여 푸시 알림을 발송한다.
        
        @param fcmToken: 수신자의 FCM 토큰
        @param messageDto: 푸시 알림 제목과 내용을 담은 DTO
        @param attempt: 현재 시도 횟수
        @return: 푸시 알림 발송 성공 여부
     */
    private boolean sendMessageWithRetry(String fcmToken, FcmMessageDto messageDto, int attempt) {
        try {
            // Firebase Cloud Messaging 메시지를 생성한다.
            Message message = Message.builder()
                    .setToken(fcmToken)  // 수신자의 FCM 토큰을 설정한다.
                    .setNotification(Notification.builder()
                            .setTitle(messageDto.getTitle())  // 알림 제목을 설정한다.
                            .setBody(messageDto.getBody())  // 알림 내용을 설정한다.
                            .build())
                    .build();

            // Firebase를 통해 푸시 알림을 발송한다.
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("푸시 알림 발송 성공 - 토큰: {}, 응답: {}", 
                    fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", response);
            return true;
        } catch (FirebaseMessagingException e) {
            // FCM 발송 중 오류가 발생한 경우 에러 코드에 따라 처리한다.
            return handleFirebaseMessagingExceptionWithRetry(e, fcmToken, messageDto, attempt);
        } catch (Exception e) {
            // 예상치 못한 오류가 발생한 경우 로그를 남긴다.
            log.error("푸시 알림 발송 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /*
        FirebaseMessagingException을 처리하고 재시도 로직을 포함한다.
        에러 코드에 따라 적절한 처리를 수행한다.
        
        @param e: FirebaseMessagingException
        @param fcmToken: 실패한 FCM 토큰
        @param messageDto: 푸시 알림 메시지 DTO
        @param attempt: 현재 시도 횟수
        @return: 푸시 알림 발송 성공 여부
     */
    private boolean handleFirebaseMessagingExceptionWithRetry(
            FirebaseMessagingException e, 
            String fcmToken, 
            FcmMessageDto messageDto, 
            int attempt
    ) {
        // 에러 코드를 문자열로 변환하여 처리한다.
        String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : "UNKNOWN";
        String errorMessage = e.getMessage();
        
        // 에러 코드에 따라 분기 처리한다.
        if (errorCode.contains("UNREGISTERED") || errorCode.contains("INVALID_TOKEN")) {
            // 앱이 삭제되었거나 토큰이 더 이상 유효하지 않은 경우
            log.warn("FCM 토큰이 등록되지 않았거나 만료되었습니다. 토큰: {}...", 
                    fcmToken.substring(0, Math.min(10, fcmToken.length())));
            
            // DB에서 해당 토큰을 가진 사용자의 FCM 토큰을 삭제한다.
            deleteExpiredToken(fcmToken);
            return false;
        } else if (errorCode.contains("INVALID_ARGUMENT")) {
            // 잘못된 인자(토큰 형식 오류 등)
            log.error("FCM 토큰 형식이 잘못되었습니다. 토큰: {}...", 
                    fcmToken.substring(0, Math.min(10, fcmToken.length())));
            return false;
        } else if (errorCode.contains("UNAVAILABLE") || errorCode.contains("INTERNAL")) {
            // Firebase 서비스가 일시적으로 사용 불가능한 경우 또는 내부 오류
            // 재시도 가능한 오류이므로 재시도 로직을 실행한다.
            return handleRetry(fcmToken, messageDto, attempt, errorCode, errorMessage);
        } else {
            // 기타 오류
            log.error("푸시 알림 발송 실패 - 에러 코드: {}, 메시지: {}", errorCode, errorMessage, e);
            return false;
        }
    }

    /*
        재시도 로직을 처리한다.
        최대 재시도 횟수 내에서 일정 간격을 두고 재시도한다.
        
        @param fcmToken: 수신자의 FCM 토큰
        @param messageDto: 푸시 알림 메시지 DTO
        @param attempt: 현재 시도 횟수
        @param errorCode: 에러 코드
        @param errorMessage: 에러 메시지
        @return: 푸시 알림 발송 성공 여부
     */
    private boolean handleRetry(String fcmToken, FcmMessageDto messageDto, int attempt, String errorCode, String errorMessage) {
        // 최대 재시도 횟수를 초과한 경우 재시도하지 않는다.
        if (attempt >= MAX_RETRY_ATTEMPTS) {
            log.error("푸시 알림 발송 실패 - 최대 재시도 횟수({}) 초과. 에러 코드: {}, 메시지: {}", 
                    MAX_RETRY_ATTEMPTS, errorCode, errorMessage);
            return false;
        }

        // 재시도 간격만큼 대기한다.
        try {
            log.warn("푸시 알림 발송 실패 - {}초 후 재시도 ({}/{}). 에러 코드: {}", 
                    RETRY_DELAY_MS / 1000, attempt + 1, MAX_RETRY_ATTEMPTS, errorCode);
            TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ie) {
            // 재시도 대기 중 인터럽트가 발생한 경우
            Thread.currentThread().interrupt();
            log.error("푸시 알림 재시도 대기 중 인터럽트 발생", ie);
            return false;
        }

        // 재시도한다.
        return sendMessageWithRetry(fcmToken, messageDto, attempt + 1);
    }

    /*
        만료된 FCM 토큰을 DB에서 삭제한다.
        해당 토큰을 가진 사용자의 FCM 토큰을 null로 설정한다.
        @Transactional을 사용하여 JPA의 더티 체킹(dirty checking)을 활용한다.
        엔티티의 필드를 변경하면 트랜잭션 커밋 시 자동으로 UPDATE가 실행된다.
        
        @param fcmToken: 만료된 FCM 토큰
     */
    @Transactional
    private void deleteExpiredToken(String fcmToken) {
        try {
            // FCM 토큰으로 사용자를 찾는다.
            userRepository.findByFcmToken(fcmToken)
                    .ifPresent(user -> {
                        // 사용자의 FCM 토큰을 삭제한다.
                        // @Transactional이 있으므로 엔티티 필드 변경 시 JPA의 더티 체킹으로 자동 UPDATE된다.
                        user.clearFcmToken();
                        // save()를 명시적으로 호출하지 않아도 트랜잭션 커밋 시 자동으로 UPDATE된다.
                        log.info("만료된 FCM 토큰을 DB에서 삭제했습니다. 사용자: {}", user.getEmail());
                    });
        } catch (Exception e) {
            // 토큰 삭제 중 오류가 발생해도 메인 로직에 영향을 주지 않도록 로그만 남긴다.
            log.error("만료된 FCM 토큰 삭제 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /*
        특정 사용자의 FCM 토큰으로 푸시 알림을 발송한다. (간편 메서드)
        
        @param fcmToken: 수신자의 FCM 토큰
        @param title: 푸시 알림 제목
        @param body: 푸시 알림 내용
        @return: 푸시 알림 발송 성공 여부
     */
    public boolean sendMessageTo(String fcmToken, String title, String body) {
        return sendMessageTo(fcmToken, new FcmMessageDto(title, body));
    }

    /*
        사용자의 FCM 토큰을 등록하거나 업데이트한다.
        
        [중요] FCM 토큰은 서버에서 자동으로 생성되지 않는다.
        FCM 토큰 생성 흐름:
        1. 클라이언트(앱)에서 Firebase SDK를 통해 FCM 토큰 생성
        2. 앱이 로그인 후 또는 앱 실행 시 생성된 토큰을 서버에 전송 (PUT /api/fcm/token)
        3. 서버는 받은 토큰을 DB에 저장만 함
        
        회원가입 시점:
        - User 엔티티 생성 시 fcmToken = null (정상)
        - 회원가입 시 FCM 토큰이 자동으로 생성되지 않음
        
        토큰 등록 시점:
        - 앱이 로그인 후 또는 앱 실행 시 Firebase SDK로 토큰 생성
        - 앱이 PUT /api/fcm/token API를 호출하여 토큰 등록
        - 이 메서드가 호출되어 DB에 토큰 저장
        
        @param email: 사용자 이메일
        @param fcmToken: Firebase Cloud Messaging 토큰 (앱에서 생성된 토큰)
     */
    @Transactional
    public void registerOrUpdateToken(String email, String fcmToken) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            // 새로운 사용자는 처음에 FCM 토큰이 null이므로, null인 경우 새로 등록한다.
                            boolean isNewRegistration = (user.getFcmToken() == null || user.getFcmToken().isBlank());
                            
                            // FCM 토큰을 등록하거나 업데이트한다.
                            // null이든 값이 있든 상관없이 user.updateFcmToken()이 정상 작동한다.
                            user.updateFcmToken(fcmToken);
                            
                            if (isNewRegistration) {
                                log.info("FCM 토큰을 새로 등록했습니다. 사용자: {} (기존 토큰: 없음)", email);
                            } else {
                                log.info("FCM 토큰을 업데이트했습니다. 사용자: {} (기존 토큰 존재)", email);
                            }
                        },
                        () -> {
                            log.warn("FCM 토큰 등록/업데이트 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        사용자의 FCM 토큰을 삭제한다.
        로그아웃 시 호출하여 더 이상 푸시 알림을 받지 않도록 한다.
        
        @param email: 사용자 이메일
     */
    @Transactional
    public void clearToken(String email) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.clearFcmToken();
                            log.info("FCM 토큰을 삭제했습니다. 사용자: {}", email);
                        },
                        () -> {
                            log.warn("FCM 토큰 삭제 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            // 사용자를 찾을 수 없어도 예외를 던지지 않는다. (로그아웃 시 이미 삭제된 경우 등)
                        }
                );
    }

    /*
        친구 관련 푸시 알림 활성화 여부를 설정한다.
        
        @param email: 사용자 이메일
        @param enabled: 푸시 알림 활성화 여부
     */
    @Transactional
    public void setFriendPushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.setFriendPushNotificationEnabled(enabled);
                            log.info("친구 관련 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}", email, enabled);
                        },
                        () -> {
                            log.warn("푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        챌린지 초대 푸시 알림 활성화 여부를 설정한다.

        @param email 사용자 이메일
        @param enabled 활성화 여부
     */
    @Transactional
    public void setChallengeInvitePushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.setChallengeInvitePushNotificationEnabled(enabled);
                            log.info("챌린지 초대 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}", email, enabled);
                        },
                        () -> {
                            log.warn("챌린지 초대 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        동기부여 메시지 푸시 알림 활성화 여부를 설정한다.

        @param email 사용자 이메일
        @param enabled 활성화 여부
     */
    @Transactional
    public void setMotivationMessagePushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            UserNotificationSchedule schedule = userNotificationScheduleRepository
                                    .findByUser_Id(user.getId())
                                    .orElseGet(() -> {//행이 없을 경우 생성
                                        LocalTime defaultTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
                                                .withMinute(0)
                                                .withSecond(0)
                                                .withNano(0);

                                        UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                                .user(user)
                                                .notificationTime(defaultTime)
                                                .enabled(false) // 기본값 false, 만약 enabled = true라면 밑에서 갱신
                                                .dailyReminderPushNotificationEnabled(true)
                                                .build();
                                        return userNotificationScheduleRepository.save(newSchedule);
                                    });

                            schedule.setEnabled(enabled);
                            log.info("동기부여 메시지 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}", email, enabled);
                        },
                        () -> {
                            log.warn("동기부여 메시지 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        전체 일일 리마인더(주간 리마인더) 푸시 알림 활성화 여부를 설정하고,
        사용자가 참여 중인 모든 챌린지의 ChallengeParticipant.dailyReminderPushEnabled 값도 함께 연쇄적으로 변경한다.

        @param email 사용자 이메일
        @param enabled 활성화 여부
     */
    @Transactional
    public void setDailyReminderPushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            UserNotificationSchedule schedule = userNotificationScheduleRepository
                                    .findByUser_Id(user.getId())
                                    .orElseGet(() -> {
                                        LocalTime defaultTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
                                                .withMinute(0)
                                                .withSecond(0)
                                                .withNano(0);

                                        UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                                .user(user)
                                                .notificationTime(defaultTime)
                                                .enabled(false)
                                                .dailyReminderPushNotificationEnabled(true)
                                                .build();
                                        return userNotificationScheduleRepository.save(newSchedule);
                                    });

                            schedule.setDailyReminderPushNotificationEnabled(enabled);
                            //유저가 참여 중인 모든 챌린지의 ChallengeParticipant.dailyReminderPushEnabled 값도 함께 연쇄적으로 변경한다.
                            if (user.getChallengeParticipants() != null) {
                                for (ChallengeParticipant participant : user.getChallengeParticipants()) {
                                    participant.setDailyReminderPushEnabled(enabled);
                                }
                            }

                            log.info("전체 일일 리마인더 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}",
                                    email, enabled);
                        },
                        () -> {
                            log.warn("전체 일일 리마인더 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        특정 챌린지에 대한 "챌린지 전체 알림"(공통 게이트) 토글을 변경하는 함수이다.
        토글이 변경되면 해당 챌린지의 개별 토글(일일 리마인더/멤버 반응/멤버 인증)도 연쇄적으로 같이 변경한다.

        @param email 사용자 이메일
        @param challengeId 챌린지 id
        @param enabled 활성화 여부
     */
    @Transactional
    public void setChallengeAllPushEnabled(String email, Long challengeId, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            ChallengeParticipant participant = challengeParticipantRepository.findByUser_IdAndChallenge_Id(user.getId(), challengeId)
                                    .orElseThrow(() -> new IllegalArgumentException("해당 챌린지에 참여 중인 사용자가 아닙니다."));

                            participant.setChallengeAllPushEnabled(enabled);
                            participant.setDailyReminderPushEnabled(enabled);
                            participant.setMemberReactionPushEnabled(enabled);
                            participant.setMemberCertificationPushEnabled(enabled);

                            log.info("개별 챌린지 전체 푸시 알림 설정을 변경했습니다. 사용자: {}, challengeId: {}, 활성화: {}",
                                    email, challengeId, enabled);
                        },
                        () -> {
                            log.warn("개별 챌린지 전체 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        특정 챌린지에 대한 "일일(주간) 리마인더" 푸시 토글을 변경하는 함수이다.

        @param email 사용자 이메일
        @param challengeId 챌린지 id
        @param enabled 활성화 여부
     */
    @Transactional
    public void setChallengeDailyReminderPushEnabled(String email, Long challengeId, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            ChallengeParticipant participant = challengeParticipantRepository.findByUser_IdAndChallenge_Id(user.getId(), challengeId)
                                    .orElseThrow(() -> new IllegalArgumentException("해당 챌린지에 참여 중인 사용자가 아닙니다."));

                            // 일일 리마인더 푸시 알림 활성화 여부를 변경한다.
                            participant.setDailyReminderPushEnabled(enabled);

                            log.info("개별 챌린지 일일 리마인더 푸시 알림 설정을 변경했습니다. 사용자: {}, challengeId: {}, 활성화: {}",
                                    email, challengeId, enabled);
                        },
                        () -> {
                            log.warn("개별 챌린지 일일 리마인더 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        특정 챌린지에 대한 "멤버 반응 소식" 푸시 토글을 변경하는 함수이다.

        @param email 사용자 이메일
        @param challengeId 챌린지 id
        @param enabled 활성화 여부
     */
    @Transactional
    public void setChallengeMemberReactionPushEnabled(String email, Long challengeId, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            ChallengeParticipant participant = challengeParticipantRepository.findByUser_IdAndChallenge_Id(user.getId(), challengeId)
                                    .orElseThrow(() -> new IllegalArgumentException("해당 챌린지에 참여 중인 사용자가 아닙니다."));

                            // 멤버 반응 소식 푸시 알림 활성화 여부를 변경한다.
                            participant.setMemberReactionPushEnabled(enabled);

                            log.info("개별 챌린지 멤버 반응 소식 푸시 알림 설정을 변경했습니다. 사용자: {}, challengeId: {}, 활성화: {}",
                                    email, challengeId, enabled);
                        },
                        () -> {
                            log.warn("개별 챌린지 멤버 반응 소식 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        특정 챌린지에 대한 "멤버 인증 소식" 푸시 토글을 변경하는 함수이다.

        @param email 사용자 이메일
        @param challengeId 챌린지 id
        @param enabled 활성화 여부
     */
    @Transactional
    public void setChallengeMemberCertificationPushEnabled(String email, Long challengeId, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            ChallengeParticipant participant = challengeParticipantRepository.findByUser_IdAndChallenge_Id(user.getId(), challengeId)
                                    .orElseThrow(() -> new IllegalArgumentException("해당 챌린지에 참여 중인 사용자가 아닙니다."));

                            // 멤버 인증 소식 푸시 알림 활성화 여부를 변경한다.
                            participant.setMemberCertificationPushEnabled(enabled);

                            log.info("개별 챌린지 멤버 인증 소식 푸시 알림 설정을 변경했습니다. 사용자: {}, challengeId: {}, 활성화: {}",
                                    email, challengeId, enabled);
                        },
                        () -> {
                            log.warn("개별 챌린지 멤버 인증 소식 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    //전체 푸시 알림 on/off
    @Transactional
    public void setAllPushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.setAllPushNotificationEnabled(enabled);
                            user.setFriendPushNotificationEnabled(enabled);
                            user.setAllMemberCertificationPushNotificationEnabled(enabled);
                            user.setAllMemberReactionPushNotificationEnabled(enabled);
                            user.setChallengeInvitePushNotificationEnabled(enabled);

                            UserNotificationSchedule schedule = userNotificationScheduleRepository
                                    .findByUser_Id(user.getId())
                                    .orElseGet(() -> {
                                        LocalTime defaultTime = LocalTime.now(ZoneId.of("Asia/Seoul"))
                                                .withMinute(0)
                                                .withSecond(0)
                                                .withNano(0);

                                        UserNotificationSchedule newSchedule = UserNotificationSchedule.builder()
                                                .user(user)
                                                .notificationTime(defaultTime)
                                                .enabled(false)
                                                .dailyReminderPushNotificationEnabled(true)
                                                .build();
                                        return userNotificationScheduleRepository.save(newSchedule);
                                    });

                            schedule.setEnabled(enabled);
                            schedule.setDailyReminderPushNotificationEnabled(enabled);

                            // 유저가 참여 중인 모든 챌린지에서도 공통 게이트 및 개별 푸시 토글을 연쇄 변경한다.
                            if (user.getChallengeParticipants() != null) {
                                for (ChallengeParticipant participant : user.getChallengeParticipants()) {
                                    participant.setChallengeAllPushEnabled(enabled);
                                    participant.setDailyReminderPushEnabled(enabled);
                                    participant.setMemberReactionPushEnabled(enabled);
                                    participant.setMemberCertificationPushEnabled(enabled);
                                }
                            }

                            log.info("전체 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}", email, enabled);
                        },
                        () -> {
                            log.warn("푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}", email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        전체 멤버 인증 소식 푸시 알림 활성화 여부를 설정하고,
        사용자가 참여 중인 모든 챌린지의 ChallengeParticipant '멤버 인증 소식' 값도 함께 연쇄적으로 변경한다.

        @param email 사용자 이메일
        @param enabled 활성화 여부
     */
    @Transactional
    public void setAllMemberCertificationPushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.setAllMemberCertificationPushNotificationEnabled(enabled);

                            if (user.getChallengeParticipants() != null) {
                                for (ChallengeParticipant participant : user.getChallengeParticipants()) {
                                    participant.setMemberCertificationPushEnabled(enabled);
                                }
                            }

                            log.info("전체 멤버 인증 소식 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}",
                                    email, enabled);
                        },
                        () -> {
                            log.warn("전체 멤버 인증 소식 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }

    /*
        전체 멤버 반응 소식 푸시 알림 활성화 여부를 설정하고,
        사용자가 참여 중인 모든 챌린지의 ChallengeParticipant '멤버 반응 소식' 값도 함께 연쇄적으로 변경한다.

        @param email 사용자 이메일
        @param enabled 활성화 여부
     */
    @Transactional
    public void setAllMemberReactionPushNotificationEnabled(String email, boolean enabled) {
        userRepository.findByEmail(email)
                .ifPresentOrElse(
                        user -> {
                            user.setAllMemberReactionPushNotificationEnabled(enabled);

                            if (user.getChallengeParticipants() != null) {
                                for (ChallengeParticipant participant : user.getChallengeParticipants()) {
                                    participant.setMemberReactionPushEnabled(enabled);
                                }
                            }

                            log.info("전체 멤버 반응 소식 푸시 알림 설정을 변경했습니다. 사용자: {}, 활성화: {}",
                                    email, enabled);
                        },
                        () -> {
                            log.warn("전체 멤버 반응 소식 푸시 알림 설정 변경 실패: 사용자를 찾을 수 없습니다. 이메일: {}",
                                    email);
                            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email);
                        }
                );
    }
}
