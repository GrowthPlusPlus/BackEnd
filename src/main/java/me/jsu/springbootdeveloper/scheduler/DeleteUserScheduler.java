// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.scheduler.schema.RequireUserDeleteSchema;
import me.jsu.springbootdeveloper.user.service.UserWithdrawalService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/*
    매일 자정마다 검사하여 유저가 회원탈퇴 상태로부터 일정 기간이 지났는지 검사하는 스케줄러이다.
 */
@Component
@RequiredArgsConstructor
public class DeleteUserScheduler {
    private final UserWithdrawalService userWithdrawalService;

    //@Scheduled(cron = "*/20 * * * * *", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @RequireUserDeleteSchema
    public void runDailyDeleteUser() {
        // DELETE 상태로 30일이 지난 계정들을 삭제한다.
        LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(30);

        // 즉시 삭제를 위해 임시로 남겨놓은 코드
        //LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        userWithdrawalService.withdraw(threshold);
    }
}