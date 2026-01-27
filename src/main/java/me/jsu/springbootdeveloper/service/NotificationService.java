// 정성우

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.notification.Notification;
import me.jsu.springbootdeveloper.domain.notification.NotificationType;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.mainHome.MyNotification;
import me.jsu.springbootdeveloper.dto.mainHome.MyNotificationResponse;
import me.jsu.springbootdeveloper.repository.NotificationRepository;
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

    /*
        유저가 읽지 않은 알람 개수를 검색해주는 함수이다.
     */
    public long countByUser_IdAndIsReadFalse(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    /*
        유저 id로 알림을 검색하는 함수이다.
        요청한 page 크기만큼 검색하여 반환한다. 현재는 사용하지 않는다.
     */
    public Page<Notification> findByUser_Id(Long userId, Pageable pageable) {
        return notificationRepository.findByUser_Id(userId, pageable);
    }

    /*
        유저 id로 알람을 검색하여 List형태로 정제하고, 더 읽을 페이지가 있는지에 대한 여부를 포함하는 dto를 생성하여 반환하는 함수이다.
        읽은 알림, 읽지 않은 알림 구분 없이 생성일을 기준으로 검색한다.
        page는 데이터를 검색하지 못하면 빈 List가 반환되므로 예외처리를 할 필요는 없다.
     */
    public MyNotificationResponse getNotification(String userEmail, int page) {
        User user = userService.findByEmail(userEmail);

        // 유저가 받은 알람을 한 번에 20개까지만 조회한다.
        Pageable pageable = PageRequest.of(
                page,           // page
                20,             // 한 페이지 당 읽을 알림 개수
                Sort.by("createdAt").descending()  // 생성일을 기준으로 내림차순으로 검색한다.
        );

        Page<MyNotification> myNotifications = notificationRepository.findByUser_Id(user.getId(), pageable)
                .map(MyNotification::new);  // 검색한 알림값을 MyNotification로 변환한다.

        return new MyNotificationResponse(
                myNotifications.getContent(),  // Page형태의 객체를 List 객체로 변환하여 저장한다.
                myNotifications.hasNext());  // 다음 page가 존재하는지에 대한 boolean값을 저장한다.
    }
}
