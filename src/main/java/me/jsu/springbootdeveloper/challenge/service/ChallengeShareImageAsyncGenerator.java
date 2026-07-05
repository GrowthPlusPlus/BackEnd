// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeCalenderShare;
import me.jsu.springbootdeveloper.challenge.exception.ShareImageNotFoundException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeCalenderShareRepository;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.post.domain.PostPicture;
import me.jsu.springbootdeveloper.post.event.CloudinaryDeleteEvent;
import me.jsu.springbootdeveloper.post.service.PostQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/*
    캘린더 공유 이미지 생성을 비동기적으로 수행하도록 하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class ChallengeShareImageAsyncGenerator {
    private static final long EXPIRE_MINUTES = 1L;
    private static final long EXPIRE_HOURS = 24L;  // 만료 시간이다.

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CalendarImageRenderService renderService;
    private final ChallengeCalenderShareRepository challengeCalenderShareRepository;
    private final ChallengeQueryService challengeQueryService;
    private final UserService userService;
    private final PostQueryService postQueryService;
    private final CloudinaryService cloudinaryService;
    private static final Logger log = LoggerFactory.getLogger(ChallengeShareImageAsyncGenerator.class);

    @Async("shareImageTaskExecutor")
    public void generate(Long shareImageId) {
        generateInternal(shareImageId);
    }

    /*
        이미지를 생성할때 사용되는 함수이다. 비동기적으로 처리된다.
     */
    @Transactional
    public void generateInternal(Long shareImageId) {
        // 작업할 공유 이미지 엔티티를 조회한다.
        ChallengeCalenderShare shareImage = challengeCalenderShareRepository.findById(shareImageId)
                .orElseThrow(() -> new ShareImageNotFoundException("SHARE_IMAGE_NOT_FOUND"));

        String postImageUrl = shareImage.getImageUrl();  // 기존 이미지 url을 저장한다.

        try {
            // 조회 기간을 계산한다. 즉 한 달 내의 기간만 조회한다.
            LocalDate startDate = LocalDate.of(shareImage.getYear(), shareImage.getMonth(), 1);
            LocalDate endDate = startDate.plusMonths(1);

            Challenge challenge = challengeQueryService.findById(shareImage.getChallenge().getId());
            User user = userService.findById(shareImage.getUser().getId());

            // 해당 월의 인증글들의 사진을 조회한다.
            List<PostPicture> proofs = postQueryService.findMonthlyProofPictures(
                    shareImage.getUser().getId(),
                    shareImage.getChallenge().getId(),
                    startDate,
                    endDate
            );

            Set<Integer> successDays = new HashSet<>();  // 캘린더 이미지에 표시할 성공 날짜를 계산한다. 여기선 인증글을 올린 날짜를 뽑아낸다.
            Map<Integer, String> proofImageUrlByDay = new HashMap<>();  // 인증 사진을 날짜에 매핑한다.
            for (PostPicture picture : proofs) {
                Integer day = picture.getArticle()
                        .getPostDate()
                        .getDayOfMonth();

                successDays.add(day);  // 성공 날짜를 계산한다.
                String imageUrl = picture.getImageUrl();

                if (imageUrl != null) {
                    proofImageUrlByDay.putIfAbsent(day, imageUrl);  // 성공 날짜에 사진을 매핑한다.
                }
            }

            // 이미지 랜더링 서비스에 정보들을 전달하여 이미지를 생성한다.
            var rendered = renderService.render(
                    challenge.getTitle(),
                    shareImage.getYear(),
                    shareImage.getMonth(),
                    successDays,
                    proofImageUrlByDay
            );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(rendered, "png", baos);  // 생성한 이미지를 png로 변환하여 baos에 저장한다.

            // 클라우디너리에 사용할 publicId를 원하는 형태로 생성한다.
            String publicId = buildPublicId(
                    user.getId(),
                    challenge.getId(),
                    shareImage.getYear(),
                    shareImage.getMonth()
            );

            // 클라우디너리에 실제로 업로드한다.
            ImageUploadResult uploaded = cloudinaryService.uploadBytes(baos.toByteArray(), publicId);

            // DB에 정보를 반영한다.
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            shareImage.markReady(uploaded.secureUrl(), uploaded.publicId(), now, now.plusHours(EXPIRE_HOURS));
            //shareImage.markReady(uploaded.secureUrl(), uploaded.publicId(), now, now.plusMinutes(EXPIRE_MINUTES));  // 테스트용 코드
            challengeCalenderShareRepository.save(shareImage);

            // 클라우디너리의 기존 이미지를 삭제한다. 모든 트렌젝션이 끝난 후에 수행한다.
            applicationEventPublisher.publishEvent(new CloudinaryDeleteEvent(postImageUrl));
        }
        catch (Exception e) {  // 전체 과정에 실패한 경우이다.
            log.error("share image generation failed", e);
            shareImage.increaseRetryCount();
        }
    }

    /*
        클라우디너리에 저장할 경로를 설정하는 함수이다.
        실제 사이트에 들어갔을때 폴더에 정리된 것처럼 사진이 보인다.
     */
    private String buildPublicId(Long userId, Long challengeId, int year, int month) {
        return "challenge-share/%d/%d/%d-%02d".formatted(userId, challengeId, year, month);
    }
}
