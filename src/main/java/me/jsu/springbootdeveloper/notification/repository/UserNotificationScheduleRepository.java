// 정성우

package me.jsu.springbootdeveloper.notification.repository;

import me.jsu.springbootdeveloper.notification.domain.UserNotificationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/*
    사용자별 동기부여 메시지 알림 시간 설정을 관리하는 리포지토리이다.
 */
public interface UserNotificationScheduleRepository extends JpaRepository<UserNotificationSchedule, Long> {
    /*
        사용자 ID로 알림 시간 설정을 조회한다.
        @param userId 사용자 ID
        @return 알림 시간 설정 (없을 경우 Optional.empty())
     */
    Optional<UserNotificationSchedule> findByUser_Id(Long userId);

    /*
        특정 시간에 알림을 받도록 설정된 활성화된 사용자들을 조회한다.
        스케줄러에서 매일 지정된 시간에 알림을 발송할 때 사용한다.
        
        @param notificationTime 알림 시간
        @return 해당 시간에 알림을 받을 활성화된 사용자들의 알림 설정 리스트
        
     */
    @Query("SELECT uns FROM UserNotificationSchedule uns " +
           "WHERE uns.enabled = true " +
           "AND uns.notificationTime = :notificationTime")
    List<UserNotificationSchedule> findEnabledSchedulesByNotificationTime(
            @Param("notificationTime") LocalTime notificationTime
    );

    /*
        활성화된 모든 알림 설정을 조회한다.
        스케줄러에서 매 시간마다 실행할 때 사용할 수 있다.
        
        @return 활성화된 모든 알림 설정 리스트
     */
    List<UserNotificationSchedule> findByEnabledTrue();
}
