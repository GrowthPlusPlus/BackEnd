// 정성우

package me.jsu.springbootdeveloper.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.challenge.Challenge;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeParticipant;
import me.jsu.springbootdeveloper.domain.challenge.ChallengeVisibility;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeParticipantRepository;
import me.jsu.springbootdeveloper.repository.challenge.ChallengeRepository;
import me.jsu.springbootdeveloper.service.UserService;
import me.jsu.springbootdeveloper.service.challenge.ChallengeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/*
    해당 클래스는 REST API 방식으로 챌린지를 생성/조회하는 기능을 제공한다.
    엔티티를 그대로 반환하면 지연 로딩, 순환 참조, 민감 정보 노출 문제가 생길 수 있으므로
    내부 DTO로 필요한 값만 정제하여 반환한다.
 */
@RequiredArgsConstructor
@RestController
public class ChallengeApiController {
    private final ChallengeService challengeService;
    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final UserService userService;

    /*
        챌린지를 생성하는 POST 요청 API이다.
        요청 본문으로 챌린지 정보를 받고, 요청자의 이메일(UserDetails)을 기반으로 오너를 설정한다.

        @param userDetails 인증된 유저 정보(이메일)
        @param request 생성할 챌린지 정보
        @return 생성된 챌린지 상세 정보(ChallengeDetailResponse)
     */
    @Transactional
    @PostMapping("/api/challenges")
    public ResponseEntity<ChallengeDetailResponse> createChallenge(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateChallengeRequest request
        ) 
    {
        validateCreateRequest(request);

        User user = userService.findByEmail(userDetails.getUsername());
        ChallengeVisibility visibility = ChallengeVisibility.valueOf(request.getChallengeVisibility());
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .user(user)
                .maxParticipantNumber(request.getMaxParticipantNumber())
                .challengeVisibility(visibility)
                .startDate(startDate)
                .endDate(endDate)
                .requiredWeeklyCount(request.getRequiredWeeklyCount())
                .build());

        // 생성자는 자동으로 참여자 1명으로 설정하지만, 참가자 테이블도 초기 엔트리를 생성한다.
        challengeParticipantRepository.save(ChallengeParticipant.builder()
                .challenge(challenge)
                .user(user)
                .build());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ChallengeDetailResponse(challenge));
    }

    /*
        챌린지 id로 챌린지 상세 정보를 조회하는 GET 요청 API이다.

        @param id 조회할 챌린지 id
        @return 조회된 챌린지 상세 정보(ChallengeDetailResponse)
     */
    @GetMapping("/api/challenges/{id}")
    public ResponseEntity<ChallengeDetailResponse> findChallengeById(
            @PathVariable Long id
    ) {
        Challenge challenge = challengeService.findById(id);
        return ResponseEntity.ok(new ChallengeDetailResponse(challenge));
    }

    /*
        공개(PUBLIC) 챌린지 목록을 조회하는 GET 요청 API이다.
        기본적으로 시작일 최신순으로 20개씩 페이징하여 반환한다.

        @param pageable 페이지 정보
        @return 공개 챌린지 목록(Page<ChallengeSummaryResponse>)
     */
    @GetMapping("/api/challenges/public")
    public ResponseEntity<Page<ChallengeSummaryResponse>> findPublicChallenges(
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChallengeSummaryResponse> response = challengeRepository
                .findByChallengeVisibilityOrderByStartDateDesc(ChallengeVisibility.PUBLIC, pageable)
                .map(ChallengeSummaryResponse::new);

        return ResponseEntity.ok(response);
    }

    /*
        진행중인 챌린지 목록을 조회하는 GET 요청 API이다.
        오늘 날짜를 기준으로 startDate <= today <= endDate 인 챌린지만 반환한다.

        @param pageable 페이지 정보
        @return 진행중인 챌린지 목록(Page<ChallengeSummaryResponse>)
     */
    @GetMapping("/api/challenges/in-progress")
    public ResponseEntity<Page<ChallengeSummaryResponse>> findInProgressChallenges(
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LocalDate today = LocalDate.now();
        Page<ChallengeSummaryResponse> response = challengeRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqual(today, today, pageable)
                .map(ChallengeSummaryResponse::new);

        return ResponseEntity.ok(response);
    }

    /*
        종료된 챌린지 목록을 조회하는 GET 요청 API이다.
        오늘 날짜를 기준으로 endDate < today 인 챌린지만 반환한다.

        @param pageable 페이지 정보
        @return 종료된 챌린지 목록(Page<ChallengeSummaryResponse>)
     */
    @GetMapping("/api/challenges/ended")
    public ResponseEntity<Page<ChallengeSummaryResponse>> findEndedChallenges(
            @PageableDefault(size = 20, sort = "endDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LocalDate today = LocalDate.now();
        Page<ChallengeSummaryResponse> response = challengeRepository
                .findByEndDateBefore(today, pageable)
                .map(ChallengeSummaryResponse::new);

        return ResponseEntity.ok(response);
    }

    /*
        챌린지 생성 요청 값의 유효성을 검사한다.
        컨트롤러에서 처리 가능한 범위의 간단한 검증만 수행한다.
     */
    private static void validateCreateRequest(CreateChallengeRequest request) {
        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(request.getStartDate());
            endDate = LocalDate.parse(request.getEndDate());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_FORMAT");
        }

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }

        try {
            ChallengeVisibility.valueOf(request.getChallengeVisibility());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CHALLENGE_VISIBILITY");
        }
    }

    /*
        챌린지 생성 요청 DTO이다.
        날짜(LocalDate)는 Jackson 설정에 따라 배열 형태로 직렬/역직렬화될 수 있으므로,
        요청/응답은 ISO-8601 문자열(yyyy-MM-dd)로 취급한다.
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChallengeRequest {
        @NotBlank
        private String title;

        @NotBlank
        private String content;

        @Min(1)
        private int maxParticipantNumber;

        @NotBlank
        private String challengeVisibility;

        @NotNull
        private String startDate;

        @NotNull
        private String endDate;

        @Min(1)
        @Max(7)
        private int requiredWeeklyCount;
    }

    /*
        챌린지 목록/요약 정보를 반환하기 위한 DTO이다.
     */
    @Getter
    public static class ChallengeSummaryResponse {
        private final Long id;
        private final String title;
        private final String content;
        private final int maxParticipantNumber;
        private final int participantNumber;
        private final String challengeVisibility;
        private final String startDate;
        private final String endDate;
        private final int requiredWeeklyCount;

        public ChallengeSummaryResponse(Challenge challenge) {
            this.id = challenge.getId();
            this.title = challenge.getTitle();
            this.content = challenge.getContent();
            this.maxParticipantNumber = challenge.getMaxParticipantNumber();
            this.participantNumber = challenge.getParticipantNumber();
            this.challengeVisibility = challenge.getChallengeVisibility().name();
            this.startDate = challenge.getStartDate().toString();
            this.endDate = challenge.getEndDate().toString();
            this.requiredWeeklyCount = challenge.getRequiredWeeklyCount();
        }
    }

    /*
        챌린지 상세 정보를 반환하기 위한 DTO이다.
        상세는 요약과 동일한 필드를 포함하되, 추후 확장 가능하도록 분리한다.
     */
    @Getter
    public static class ChallengeDetailResponse {
        private final Long id;
        private final String title;
        private final String content;
        private final int maxParticipantNumber;
        private final int participantNumber;
        private final String challengeVisibility;
        private final String startDate;
        private final String endDate;
        private final int requiredWeeklyCount;

        public ChallengeDetailResponse(Challenge challenge) {
            this.id = challenge.getId();
            this.title = challenge.getTitle();
            this.content = challenge.getContent();
            this.maxParticipantNumber = challenge.getMaxParticipantNumber();
            this.participantNumber = challenge.getParticipantNumber();
            this.challengeVisibility = challenge.getChallengeVisibility().name();
            this.startDate = challenge.getStartDate().toString();
            this.endDate = challenge.getEndDate().toString();
            this.requiredWeeklyCount = challenge.getRequiredWeeklyCount();
        }
    }
}
