// 정성우

package me.jsu.springbootdeveloper.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPhotoResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarResponse;
import me.jsu.springbootdeveloper.view.dto.ChallengeCalendarPostResponse;
import me.jsu.springbootdeveloper.view.service.ChallengeCalendarService;
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
        챌린지 총 완료 일수, 연속 일수를 조회한다. 또한 챌린지 방장인지 아닌지에 대한 여부도 반환한다.
     */
    @Operation(
            summary = "챌린지 달력 상단 정보 조회",
            description = """
        챌린지 달력 상단에 표시되는 기본 정보를 조회한다. 총 완료 일수와 연속 완료 일수를 반환한다.
        또한 챌린지의 방장인지 아닌지에 대한 정보를 반환한다.
        해당 정보로 사용자가 설정을 눌렀을 때, 방장이면 챌린지 설정이 보이도록 한다.
        ex) GET /api/challenges/1/calendar
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "달력 요약 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/calendar")
    public ResponseEntity<ChallengeCalendarResponse> getCalendarPhotos(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "현재 조회하는 챌린지 ID", required = true)
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
    @Operation(
            summary = "챌린지 달력 중단의 챌린지 인증 여부 조회 + 사진",
            description = """
        특정 연도와 월에 해당하는 챌린지 인증 여부, 사진 목록을 반환한다.
        달력 UI 표시 용도로 사용된다.
        ex) GET /api/challenges/1/calendar/photos?year=2025&month=7
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "달력 챌린지 인증 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/calendar/photos")
    public ResponseEntity<List<ChallengeCalendarPhotoResponse>> getCalendarPhotos(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "현재 조회하는 챌린지 ID", required = true)
            @PathVariable Long challengeId,
            @Parameter(description = "현재 선택한 달력 연도 (예: 2025)", required = true, example = "2025")
            @RequestParam int year,
            @Parameter(description = "현재 선택한 달력 월 (1~12)", required = true, example = "7")
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
        클라이언트는 페이징된 값들 뿐만아니라 여러 정보를 함께 알 수 있다.
        totalElements: 총 게시글 개수, totalPages: 총 페이지 개수, last: 해당 페이지가 마지막 페이지인가? 등의 정보이다.
     */
    @Operation(
            summary = "챌린지 달력 하단의 인증글 목록 조회",
            description = """
        해당 챌린지 인증글 목록을 페이지로 조회한다.
        연 월에 따라 해당 챌린지에 대해 수행한 인증글을 작성 순서대로 조회하여 반환한다.
        Page 객체를 반환하며, totalElements, totalPages, last 등의 정보를 포함한다.
        사용 편의를 위해 page를 사용하므로 page는 항상 0이다.
        ex) /api/challenges/1/calendar/posts?year=2025&month=7&page=0
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증글 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "챌린지를 찾을 수 없음")
    })
    @GetMapping("/api/challenges/{challengeId}/calendar/posts")
    public ResponseEntity<Page<ChallengeCalendarPostResponse>> getPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "현재 조회하는 챌린지 ID", required = true)
            @PathVariable Long challengeId,
            @Parameter(description = "조회할 연도", required = true, example = "2025")
            @RequestParam int year,
            @Parameter(description = "조회할 월 (1~12)", required = true, example = "7")
            @RequestParam int month,
            @Parameter(description = "페이지 번호 (현재는 0만 사용)", required = true, example = "0")
            @RequestParam int page
    ) {
        Page<ChallengeCalendarPostResponse> response = challengeCalendarService.getPostPhotos(
                        userDetails.getUsername(),
                        challengeId,
                        year,
                        month,
                        page
                );

        return ResponseEntity.ok(response);
    }
}
