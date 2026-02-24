// 정성우

package me.jsu.springbootdeveloper.notification.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.notification.domain.Notification;
import me.jsu.springbootdeveloper.notification.enumType.NotificationType;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import me.jsu.springbootdeveloper.notification.dto.MyNotificationResponse;
import me.jsu.springbootdeveloper.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        notification.setImageUrl(imageUrl);
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
        notification.setImageUrl(imageUrl);
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
        유저 id로 알람을 검색하여 페이지 형태로 반환하는 함수이다.
     */
    public Page<MyNotificationResponse> getNotification(String email, int page) {
        User user = userService.findByEmail(email);

        // 유저가 받은 알람을 한 번에 20개까지만 조회한다.
        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 알림 개수
                Sort.by("createdAt").descending()  // 생성일을 기준으로 내림차순으로 검색한다.
        );

        // 알림을 검색한다.
        Page<Notification> notifications = notificationRepository.findByUser_Id(user.getId(), pageable);

        // dto에 매핑하여 반환한다.
        return notifications.map(notification ->
                        MyNotificationResponse.builder()
                                .imageUrl(notification.getImageUrl())
                                .targetId(notification.getTargetId())
                                .message(notification.getMessage())
                                .type(notification.getType())
                                .isRead(notification.isRead())
                                .created(notification.getCreatedAt().toLocalDate())
                                .build());
    }
}
