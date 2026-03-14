// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.domain.ChallengeCalenderShare;
import me.jsu.springbootdeveloper.challenge.dto.share.CalenderShareResponse;
import me.jsu.springbootdeveloper.challenge.enumType.ShareImageStatus;
import me.jsu.springbootdeveloper.challenge.exception.InvalidDateException;
import me.jsu.springbootdeveloper.challenge.repository.ChallengeCalenderShareRepository;
import me.jsu.springbootdeveloper.post.event.CloudinaryDeleteEvent;
import me.jsu.springbootdeveloper.post.service.PostQueryService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/*
    공유할 캘린더 이미지를 생성하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class ChallengeShareImageService {
    private final ChallengeCalenderShareRepository challengeCalenderShareRepository;
    private final ChallengeQueryService challengeQueryService;
    private final UserService userService;
    private final PostQueryService postQueryService;
    private final ChallengeShareImageAsyncGenerator asyncGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;

    /*
        유저가 공유 이미지를 요청할 때 사용하는 함수이다.
        DB에 기존 이미지가 존재한다면 상태를 확인한 뒤 동일하다면 그대로 반환하고, 없다면 새로 비동기 생성한다.
     */
    @Transactional
    public CalenderShareResponse createOrGet(String email, Long challengeId, int year, int month) {
        User user = userService.findByEmail(email);

        validateMonth(year, month);  // 들어온 년도와 월이 올바른 값인지 검사한다.
        challengeQueryService.findByUser_IdAndChallenge_IdForParticipant(user.getId(), challengeId);  // 챌린지에 참여한 유저만 이미지 생성이 가능하다.

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 기존에 생성한 이미지가 있는지 검색한다.
        ChallengeCalenderShare challengeCalenderShare = findCalender(user.getId(), challengeId, year, month);

        if (challengeCalenderShare != null) {  // 기존에 사진이 있는 경우이다.
            // 이미지 생성 이후 새로운 인증글이나 수정사항이 있는지 검사한다.
            boolean hasNewProof = postQueryService.existsNewProof(user.getId(), challengeId, challengeCalenderShare.getUpdateAt());
            if (!hasNewProof && challengeCalenderShare.getStatus() == ShareImageStatus.READY) {  // 업데이트도 없고 사진도 준비된 경우
                return CalenderShareResponse.builder()  // 사진을 반환한다.
                        .challengeId(challengeId)
                        .imageUrl(challengeCalenderShare.getImageUrl())
                        .updateAt(challengeCalenderShare.getUpdateAt())
                        .status(challengeCalenderShare.getStatus())
                        .build();
            }

            // 업데이트가 있는 경우, 상태를 처리 상태로 바꾸고 비동기로 이미지 생성을 요청한다. 이미 처리 상태인경우 아무런 행동도 하지 않는다.
            if (challengeCalenderShare.getStatus() == ShareImageStatus.READY) {
                challengeCalenderShare.markProcessing(year, month, now);  // 처리 상태로 변경한다.

                asyncGenerator.generate(challengeCalenderShare.getId());  // 비동기적으로 이미지를 생성한다.
            }
        }
        else {
            Challenge challenge = challengeQueryService.findById(challengeId);

            // 기존에 존재하지 않는 경우 새로 생성한다.
            challengeCalenderShare = ChallengeCalenderShare.builder()
                    .user(user)
                    .challenge(challenge)
                    .year(year)
                    .month(month)
                    .updateAt(now)
                    .build();
            challengeCalenderShareRepository.save(challengeCalenderShare);

            // 비동기적으로 이미지를 생성한다.
            asyncGenerator.generate(challengeCalenderShare.getId());
        }

        // 이 코드를 실행하는 경우 아직 이미지를 생성하는 중이다. 이미지가 존재하는 경우 첫 if문에서 반환한다.
        return CalenderShareResponse.builder()
                .challengeId(null)
                .imageUrl(null)
                .updateAt(null)
                .status(ShareImageStatus.PROCESSING)
                .build();
    }

    /*
        오랜 시간이 지난 이미지를 삭제하는 함수이다. 클라우디너리의 사진과 DB값 모두 삭제한다.
     */
    @Transactional
    public void deleteExpiredImages() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        List<ChallengeCalenderShare> expired = challengeCalenderShareRepository.findImagesForCleanup(now);  // 만료된 사진을 찾는다.

        if (!expired.isEmpty()) {
            for (ChallengeCalenderShare image : expired) {
                String publicId = image.getCloudinaryPublicId();

                // 우리 서버에서 이미지 정보를 삭제한다.
                challengeCalenderShareRepository.delete(image);

                // 클라우디너리의 이미지를 삭제한다. 모든 트렌젝션이 끝난 후에 수행한다.
                applicationEventPublisher.publishEvent(new CloudinaryDeleteEvent(publicId));
            }
        }
    }

    /*
        일정 시간 이상동안 처리되지 않은 비동기 작업들을 재실행하는 함수이다.
     */
    @Transactional
    public void retryStuckProcessingImages() {
        LocalDateTime threshold = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(10);  // 10분 이상 처리되지 않은 작업들을 재시도한다.

        List<ChallengeCalenderShare> stuck = challengeCalenderShareRepository.findStuckProcessingImages(ShareImageStatus.PROCESSING, threshold);

        for (ChallengeCalenderShare image : stuck) {
            asyncGenerator.generate(image.getId());  // 다시 비동기 처리한다.
        }
    }

    /*
        기존의 캘린더 공유 DB 값을 조회한다.
     */
    public ChallengeCalenderShare findCalender(Long userId, Long challengeId, int year, int month) {
        return challengeCalenderShareRepository.findByUser_IdAndChallenge_IdAndYearAndMonth(userId, challengeId, year, month)
                .orElse(null);
    }

    /*
        들어온 월과 연도가 정상적인 값인지 검사한다.
     */
    private void validateMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new InvalidDateException("INVALID_MONTH");
        }
        if (year < 2000 || year > 2100) {
            throw new InvalidDateException("INVALID_YEAR");
        }
    }
}