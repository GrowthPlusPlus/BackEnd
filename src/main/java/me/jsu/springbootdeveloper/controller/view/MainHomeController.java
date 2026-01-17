// 정성우

package me.jsu.springbootdeveloper.controller.view;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.mainHome.MainHomeResponse;
import me.jsu.springbootdeveloper.service.view.MainHomeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;

/*
    메인화면을 구성하기 위한 요소를 반환해주는 컨트롤러이다.
    오직 조회에 관한 책임만 존재하기 때문에 수정, 삭제와 같은 행위를 담당해서는 안된다.
    선택한 날짜에 따라 다른 정보를 줘야 하므로 GET /api/mainHome?date=2025-12-25 의 형태를 지켜서 클라이언트가 값을 보내야 한다.
 */
@RestController
@RequiredArgsConstructor
public class MainHomeController {
    private final MainHomeService mainHomeService;

    @GetMapping("/api/mainHome")
    public ResponseEntity<MainHomeResponse> getMainHome(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate  // 날짜를 받는다.
    ) {

        LocalDate today = LocalDate.now();  // 해당 요청이 처리되는 기준이 되는 날짜를 생성한다.
        LocalDate weekStartDate = targetDate.with(DayOfWeek.MONDAY);  // 요청 날짜 주의 챌린지 수행 로그를 검색하기 위한 값이다.

        // 서비스에서 정제한 정보를 response 객체에 담는다.
        MainHomeResponse response = mainHomeService.getMainHome(userDetails.getUsername(), today, weekStartDate, targetDate);

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }
}
