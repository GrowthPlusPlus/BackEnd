// 정성우

package me.jsu.springbootdeveloper.scheduler;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.post.service.ImageTemporaryService;
import me.jsu.springbootdeveloper.scheduler.schema.RequireTempImageSchema;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

/*
    일정 주기로 사용되지 않은 임시 사진 파일들을 삭제하는 스케줄러이다.
 */
@Component
@RequiredArgsConstructor
public class TempImageScheduler {
    private final ImageTemporaryService imageTemporaryService;

    /*
        6시간 주기로 사용되지 않는 임시 파일들을 삭제한다.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)  // 6시간마다 실행된다.
    @RequireTempImageSchema
    @Transactional
    public void cleanupTempImages() {
        // 현재 절대 시각보다 1시간 이전에 생성된 임시 파일들을 삭제하는 threshold를 생성한다.
        Instant threshold = Instant.now().minus(1, HOURS);

        List<PostPictureTemporary> expired = imageTemporaryService.findExpired(threshold);

        // 만료된 파일들을 삭제한다.
        for (PostPictureTemporary temp : expired) {
            imageTemporaryService.deleteImages(temp);
        }
    }
}
