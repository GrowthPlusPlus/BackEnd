// 정성우

package me.jsu.springbootdeveloper.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.Notification;
import me.jsu.springbootdeveloper.domain.User;
import me.jsu.springbootdeveloper.dto.mainHome.MyNotification;
import me.jsu.springbootdeveloper.dto.mainHome.MyNotificationResponse;
import me.jsu.springbootdeveloper.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/*
    알람을 관리하기 위한 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    /*
        유저가 읽지 않은 알람 개수를 검색해주는 함수이다.
     */
    public long countByUser_IdAndIsReadFalse(Long userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    /*
        유저 id로 알림을 검색하는 함수이다.
        요청한 page 크기만큼 검색하여 반환한다.
     */
    public Page<Notification> findByUser_Id(Long userId, Pageable pageable) {
        return notificationRepository.findByUser_Id(userId, pageable);
    }

    /*
        유저 id로 알람을 검색하여 List형태로 정제하고, 더 읽을 페이지가 있는지에 대한 여부를 포함하는 dto를 생성하여 반환하는 함수이다.
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
