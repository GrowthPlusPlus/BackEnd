// 정성우

package me.jsu.springbootdeveloper.view.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.view.dto.MainHomeResponse;
import me.jsu.springbootdeveloper.view.service.MainHomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;

/*
    메인화면을 구성하기 위한 요소를 반환해주는 컨트롤러이다.
    오직 조회에 관한 책임만 존재하기 때문에 수정, 삭제와 같은 행위를 담당해서는 안된다.
    선택한 날짜에 따라 다른 정보를 줘야 하므로 GET /api/mainHome?date=2025-12-25 의 형태를 지켜서 클라이언트가 값을 보내야 한다.
 */
@RestController
@RequiredArgsConstructor
public class MainHomeController {
    private final MainHomeService mainHomeService;

    @Operation(
            summary = "메인 홈 화면 데이터 조회",
            description = """
        메인 홈 화면에 필요한 정보를 조회한다.
        기준 날짜(date)를 받아 해당 날짜가 포함된 주간 데이터와 당일 상태 정보를 함께 반환한다.
        ex) GET /api/mainHome?date=2025-07-21
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메인 홈 데이터 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "유저 또는 관련 데이터 없음")
    })
    @GetMapping("/api/mainHome")
    public ResponseEntity<MainHomeResponse> getMainHome(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(
                    description = "기준 날짜 (ISO-8601 형식, yyyy-MM-dd)",
                    required = true,
                    example = "2025-07-21"
            )
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate  // 날짜를 받는다.
    ) {
        // 서비스에서 정제한 정보를 response 객체에 담는다.
        MainHomeResponse response = mainHomeService.getMainHome(userDetails.getUsername(), targetDate);

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }
}
