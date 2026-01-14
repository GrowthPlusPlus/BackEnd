// 정성우

package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.mainHome.MainHomeResponse;
import me.jsu.springbootdeveloper.service.view.MainHomeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;

/*
    메인화면을 구성하기 위한 요소를 반환해주는 컨트롤러이다.
    오직 조회에 관한 책임만 존재하기 때문에 수정, 삭제와 같은 행위를 담당해서는 안된다.
 */
@RestController
@RequiredArgsConstructor
public class MainHomeController {
    private final MainHomeService mainHomeService;

    @GetMapping("/api/mainHome")
    public ResponseEntity<MainHomeResponse> getMainHome(@AuthenticationPrincipal UserDetails userDetails) {

        LocalDate today = LocalDate.now();  // 해당 요청이 처리되는 기준이 되는 날짜를 생성한다.
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);  // 이번주의 챌린지 수행 로그를 검색하기 위한 값이다.

        // 서비스에서 정제한 정보를 response 객체에 담는다.
        MainHomeResponse response = mainHomeService.getMainHome(userDetails.getUsername(), today, weekStartDate);

        // 검색한 값을 클라이언트에게 반환한다.
        return ResponseEntity.ok(response);
    }
}
