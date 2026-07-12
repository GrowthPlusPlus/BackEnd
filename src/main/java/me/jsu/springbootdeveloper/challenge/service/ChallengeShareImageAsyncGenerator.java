// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeCalenderShare;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeParticipant;
import me.jsu.springbootdeveloper.challenge.dto.share.DayInfo;
import me.jsu.springbootdeveloper.challenge.dto.share.ShareImageRenderRequest;
import me.jsu.springbootdeveloper.challenge.exception.ShareImageNotFoundException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeCalenderShareRepository;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.post.domain.Article;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final HtmlCaptureService htmlCaptureService;
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

            ChallengeParticipant challengeParticipant = challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challenge.getId());
            int totalSuccessDays = challengeParticipant.getTotalSuccessDays();  // 해당 챌린지에 대한 총 성공 횟수를 조회한다.

            // 해당 월의 인증글들을 조회한다.
            List<Article> articles = postQueryService.findMonthlyProofArticles(
                    shareImage.getUser().getId(),
                    shareImage.getChallenge().getId(),
                    startDate,
                    endDate
            );

            Set<Integer> successDays = new HashSet<>();  // 캘린더 이미지에 표시할 성공 날짜를 계산한다. 여기선 인증글을 올린 날짜를 뽑아낸다.
            Map<Integer, String> proofImageUrlByDay = new HashMap<>();  // 인증 사진을 날짜에 매핑한다.
            for (Article article : articles) {
                Integer day = article.getPostDate().getDayOfMonth();
                successDays.add(day);  // 성공 날짜를 계산한다.

                if (!article.getPostPictures().isEmpty()) {
                    String imageUrl = article.getPostPictures().get(0).getImageUrl();  // 사진 한 장만 사용한다.

                    if (imageUrl != null) {
                        proofImageUrlByDay.putIfAbsent(day, imageUrl);  // 성공 날짜에 사진을 매핑한다.
                    }
                }
            }

            // 맨 위 상단에 표시할 달이다.
            String[] months = {
                    "JAN","FEB","MAR","APR",
                    "MAY","JUN","JUL","AUG",
                    "SEP","OCT","NOV","DEC"
            };

            // 맨 위 상단에 표시할 텍스트이다.
            String badgeText =
                    months[shareImage.getMonth() - 1]
                            + " · "
                            + shareImage.getYear()
                            + " · CHALLENGE";

            // 이번 달의 첫 날짜를 생성한다. 따라서 이번 달이 며칠까지 있는지, 1일이 무슨 요일인지 할 수 있다.
            LocalDate firstDay = LocalDate.of(
                    shareImage.getYear(),
                    shareImage.getMonth(),
                    1
            );

            // 이번 달의 마지막 날짜를 계산한다.
            int lastDay = firstDay.lengthOfMonth();

            // HTML은 0=일요일, 1=월요일 ...로 계산하기 위해 사용한다.
            int startDayOfWeek = firstDay.getDayOfWeek().getValue() % 7;

            // 날짜 리스트를 생성한다.
            List<DayInfo> days = new ArrayList<>();
            // 모든 날짜를 하나씩 검사한다.
            for (int day = 1; day <= lastDay; day++) {
                String status = "not-done";
                String photoUrl = null;

                // 성공한 날이라면 사진 또는, 했다는 표시를 한다.
                if (successDays.contains(day)) {
                    photoUrl = proofImageUrlByDay.get(day);

                    status = (photoUrl != null) ? "photo" : "done";
                }

                // dto를 생성한다.
                days.add(
                        DayInfo.builder()
                                .date(day)
                                .status(status)
                                .photoUrl(photoUrl)
                                .build()
                );
            }

            // yyyy MM dd 형태로 날짜를 표시하기 위한 포맷터이다.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd");

            ShareImageRenderRequest request =
                    ShareImageRenderRequest.builder()
                            .badgeText(badgeText)
                            .title(challenge.getTitle())
                            .monthlyDone(successDays.size())
                            .monthlyTotal(lastDay)   // 이번 달 총 날짜 수
                            .startDate(challengeParticipant.getJoinDate().format(formatter))
                            .totalDone(totalSuccessDays)
                            .startDayOfWeek(startDayOfWeek)
                            .days(days)
                            .build();

            // 이미지를 생성하고 캡처한 값을 얻는다.
            byte[] image = htmlCaptureService.capture(request);

            // 클라우디너리에 사용할 publicId를 원하는 형태로 생성한다.
            String publicId = buildPublicId(
                    user.getId(),
                    challenge.getId(),
                    shareImage.getYear(),
                    shareImage.getMonth()
            );

            // 클라우디너리에 실제로 업로드한다.
            ImageUploadResult uploaded = cloudinaryService.uploadBytes(image, publicId);

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
