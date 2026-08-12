// 정성우

package me.jsu.springbootdeveloper.notification.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.notification.domain.Notification;
import me.jsu.springbootdeveloper.notification.dto.MyNotificationResponse;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.notification.repository.NotificationRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
    알람을 관리하기 위한 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    /**
     * 인앱 알림을 저장하고 반환한다
     */
    @Transactional
    public void sendNotification(User user, NotificationType notificationType, String message) {
        // 1. 인앱 알림 DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(notificationType)
                .build();
        notificationRepository.save(notification);

        /* 2. 실시간 푸시 알림 발송(구현 예정)
        // user 객체에 fcmToken 필드가 있다고 가정합니다.
        String title = "새로운 챌린지 알림";
        fcmPushService.sendMessageTo(user.getFcmToken(), title, message);
        */
    }

    /*
        프로필 이미지 URL이 없거나 빈 문자열이면 null로 저장한다.
        클라이언트는 profileImageUrl이 null인 경우 기본 이미지를 표시하면 된다.
     */
    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        return imageUrl;
    }

    //친구 관련 알림 등 imageUrl 필요, targetId 필요하지 않을 경우 호출
    @Transactional
    public void sendNotification(User user, NotificationType notificationType, String message, String imageUrl ) {
        // 1. 인앱 알림 DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(notificationType)
                .build();
        //이미지 url 설정
        notification.setImageUrl(normalizeImageUrl(imageUrl));
        notificationRepository.save(notification);

    }

    //챌린지, 게시글 관련 타겟id 필요할 경우 호출
    @Transactional
    public void sendNotification(User user, NotificationType notificationType, String message, String imageUrl, Long targetId ) {
        // 1. 인앱 알림 DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .type(notificationType)
                .build();
        //이미지 url 설정
        notification.setImageUrl(normalizeImageUrl(imageUrl));
        //타겟 id 설정
        notification.setTargetId(targetId);
        notificationRepository.save(notification);

    }
    /*
        유저가 읽지 않은 알람 개수를 검색해주는 함수이다.
        mainHome에서 사용한다.
     */
    public long countByUser_IdAndIsReadFalse(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    /*
        유저 id로 알람을 검색하여 DTO 형태로 반환하는 함수이다.
        반환된 알림들은 자동으로 읽음 처리되지만, DTO에는 읽음 처리 전 원래 상태가 담긴다.
        한 번의 DB 조회로 성능을 최적화한다.

        DB 일시 장애(DataAccessException 등)가 발생할 수 있으므로, @Retryable을 사용해
        동일 요청을 최대 3번까지 재시도한다. (지수 백오프 적용)
     */
    @Transactional
    @Retryable(
            // DataAccessException은 스프링에서 제공하는 DB 관련 런타임 예외의 상위 타입이다.
            value = {DataAccessException.class, RuntimeException.class},
            maxAttempts = 3,  // 최대 3번까지 재시도한다.
            backoff = @org.springframework.retry.annotation.Backoff(
                    delay = 200,           // 최초 대기 시간(밀리초)
                    multiplier = 2.0      // 재시도마다 대기 시간을 2배씩 증가시킨다. (200ms → 400ms → 800ms)
            )
    )
    public Page<MyNotificationResponse> getNotifications(String email, int page) {
        User user = userService.findByEmail(email);

        // 유저가 받은 알람을 한 번에 20개까지만 조회한다.
        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 알림 개수
                Sort.by("createdAt").descending()  // 생성일을 기준으로 내림차순으로 검색한다.
        );

        // 알림을 검색한다.
        Page<Notification> notifications = notificationRepository.findByUser_Id(user.getId(), pageable);

        // DTO로 변환한다. 이 시점의 isRead 상태(읽음 처리 전 원래 상태)를 DTO에 담는다.
        Page<MyNotificationResponse> response = notifications.map(notification ->
                        MyNotificationResponse.builder()
                                .profileImageUrl(notification.getImageUrl())
                                .targetId(notification.getTargetId())
                                .message(notification.getMessage())
                                .type(notification.getType())
                                .isRead(notification.isRead())  // 읽음 처리 전 원래 상태
                                .created(notification.getCreatedAt().toLocalDate())
                                .build());

        // 반환된 알림들을 읽음 처리한다.
        // DTO는 이미 생성되었으므로 원래 isRead 상태가 유지된다.
        // 이미 읽은 알림도 markAsRead()를 호출해도 결과는 동일하므로 필터링 없이 모든 알림을 처리한다.
        notifications.getContent().forEach(Notification::markAsRead);  // 읽음 처리
        return response;
    }

    /*
        getNotifications 함수의 재시도가 모두 실패했을 때 호출되는 복구 함수이다.
        @Recover 메서드는 첫 번째 인자로 예외 타입을 받고, 나머지 인자는 @Retryable 메서드와 동일해야 한다.

        여기서는 로그성 처리를 위해 예외 메시지를 참고하고,
        클라이언트에게는 "빈 페이지"를 반환하여 서비스 전체 장애로 번지지 않도록 방어한다.
     */
    
    @Recover
    public Page<MyNotificationResponse> recoverGetNotifications(
            RuntimeException exception, String email, int page) {

        
        // 2. 클라이언트에게는 "정상적인 빈 응답"을 반환하여 화면이 깨지지 않도록 한다.
        Pageable pageable = PageRequest.of(
                page,
                20,
                Sort.by("createdAt").descending()
        );

        return new PageImpl<>(
                java.util.List.of(),  // 빈 리스트
                pageable,
                0                     // 전체 개수 0
        );
    }


    /*
        프론트에서 사용자가 여러 알림을 한 번에 읽음 처리할 수 있도록 하는 함수이다.
        알림 ID 리스트와 사용자 이메일을 받아서,
        해당 사용자의 알림들만 한 번에 읽음 처리한다.

        정책 B: 요청한 ID 중 일부가 존재하지 않거나 다른 사용자의 알림이어도
        예외를 던지지 않고, 내가 가진 알림만 읽음 처리한다.

        @param email 사용자 이메일 (인증 정보)
        @param notificationIds 읽음 처리할 알림 ID 리스트
     */
    @Transactional
    public void markNotificationsAsRead(String email, List<Long> notificationIds) {
        // 1. 빈 리스트인 경우 조기 반환한다.
        if (notificationIds == null || notificationIds.isEmpty()) {
            return;
        }

        // 2. 사용자 정보를 조회한다.
        User user = userService.findByEmail(email);

        // 3. 여러 알림을 쿼리 한 번으로 읽음 처리한다.
        //    - 내가 가진 알림만 읽음 처리되고, 나머지는 무시된다.
        notificationRepository.markAsReadByIdsAndUserId(
                user.getId(),
                notificationIds
        );
    }
}
