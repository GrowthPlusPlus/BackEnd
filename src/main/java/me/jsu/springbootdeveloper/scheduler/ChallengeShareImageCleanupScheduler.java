// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.service.ChallengeShareImageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
    일정 주기마다 공유한 이미지를 삭제하고, 오랜 시간동안 생성하지 못한 이미지를 재생성하는 함수이다.
 */
@Component
@RequiredArgsConstructor
public class ChallengeShareImageCleanupScheduler {
    private final ChallengeShareImageService shareImageService;

    // 주기적으로 오래된 공유 이미지를 삭제한다.
    //@Scheduled(cron = "*/20 * * * * *", zone = "Asia/Seoul")
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredShareImages() {
        shareImageService.deleteExpiredImages();
    }

    // processing 상태로 오랫동안 멈춘 작업을 찾아서 재시도한다.
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void markStuckProcessingAsFailed() {
        shareImageService.retryStuckProcessingImages();
    }
}
