// 정성우

package me.jsu.springbootdeveloper.notification.repository;

import me.jsu.springbootdeveloper.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

    /*
        단일 알림을 쿼리 한 번으로 읽음 처리하기 위한 메서드이다.
        주어진 userId에 속한 notificationId만 isRead 값을 true로 변경한다.
        이미 읽은(isRead = true) 알림은 다시 업데이트하지 않는다.

        정책 B: 요청한 알림이 존재하지 않거나 다른 사용자의 알림이어도 예외를 던지지 않고,
        단순히 업데이트되는 행이 0건이면 아무 일도 일어나지 않은 것으로 처리한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.isRead = true
             where n.user.id = :userId
               and n.id = :notificationId
               and n.isRead = false
            """)
    int markAsReadByIdAndUserId(
            @Param("userId") Long userId,
            @Param("notificationId") Long notificationId
    );

    /*
        여러 알림을 한 번에 읽음 처리하기 위한 메서드이다.
        주어진 userId에 속한 notificationIds만 isRead 값을 true로 변경한다.
        이미 읽은(isRead = true) 알림은 다시 업데이트하지 않는다.

        정책 B: 존재하지 않거나 다른 사용자의 알림 ID가 섞여 있어도,
        내가 가진 알림만 읽음 처리하고 나머지는 무시한다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
               set n.isRead = true
             where n.user.id = :userId
               and n.id in :notificationIds
               and n.isRead = false
            """)
    void markAsReadByIdsAndUserId(
            @Param("userId") Long userId,
            @Param("notificationIds") List<Long> notificationIds
    );
}
