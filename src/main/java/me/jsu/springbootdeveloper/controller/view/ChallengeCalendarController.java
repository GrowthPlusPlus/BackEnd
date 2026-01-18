// 정성우

package me.jsu.springbootdeveloper.controller.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarResponse;
import me.jsu.springbootdeveloper.dto.challengeCalendar.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.service.view.ChallengeCalendarService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
    챌린지 별 한 달간의 기록을 보여줄 수 있게 화면에 필요한 정보를 보내는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
public class ChallengeCalendarController {
    private final ChallengeCalendarService challengeCalendarService;

    /*
        상단의 기본 정보를 보여준다. 한 번만 조회한다.
        챌린지 총 완료 일수, 연속 일수를 조회한다.
     */
    @GetMapping("/api/challenges/{challengeId}/calendar")
    public ResponseEntity<ChallengeCalendarResponse> getCalendarPhotos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId
    ) {
        ChallengeCalendarResponse response = challengeCalendarService.getCalendar(
                userDetails.getUsername(),
                challengeId
        );

        return ResponseEntity.ok(response);
    }

    /*
        달력용으로 한 달간의 챌린지 인증 날짜의 사진을 조회한다.
        GET /api/challenges/{challengeId}/calendar/photos?year=2025&month=7 해당 링크로 클라이언트에게 요청을 보낸다.
     */
    @GetMapping("/api/challenges/{challengeId}/calendar/photos")
    public ResponseEntity<List<ChallengeCalendarPhotoResponse>> getCalendarPhotos(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<ChallengeCalendarPhotoResponse> response = challengeCalendarService.getCalendarPhotos(
                        userDetails.getUsername(),
                        challengeId,
                        year,
                        month
                );

        return ResponseEntity.ok(response);
    }

    /*
        인증글 목록을 page로 가져온다.
     */
    @GetMapping("/api/challenges/{challengeId}/calendar/posts")
    public ResponseEntity<Page<ChallengeCalendarPostResponse>> getPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestParam int page
    ) {
        Page<ChallengeCalendarPostResponse> response = challengeCalendarService.getPostPhotos(
                        userDetails.getUsername(),
                        challengeId,
                        page
                );

        return ResponseEntity.ok(response);
    }
}
