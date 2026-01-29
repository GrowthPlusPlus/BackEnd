// 정성우

package me.jsu.springbootdeveloper.notification.repository;

import me.jsu.springbootdeveloper.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/*
    알람을 관리하는 DB로, 테이블에 매핑되는 엔티티 타입은 Notification, 엔티티의 pk 타입은 Long이다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 유저 id를 기준으로 알림을 조회한다. page를 이용하여 전체에서 일부분을 조회한다.
    Page<Notification> findByUser_Id(
            Long userId,
            Pageable pageable
    );
    long countByUser_IdAndIsReadFalse(Long userId);  // 읽지 않은 알림을 유저 id로 조회한다.
}
