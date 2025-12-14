package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.CreateAccessTokenRequest;
import me.jsu.springbootdeveloper.dto.CreateAccessTokenResponse;
import me.jsu.springbootdeveloper.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TokenApiController {
    private final TokenService tokenService;

    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(@RequestBody CreateAccessTokenRequest request) {
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());  // 리프레시 토큰으로 엑세스 토큰 생성

        /*
        ResponseEntity -> http 응답 전체를 직접 정의함
        .status(HttpStatus.CREATED) -> http의 상태코드 201로 요청이 성공적으로 처리 되었다는 의미
        .body -> http의 JSON 바디에 엑세스 토큰을 담아 클라이언트에게 응답함
         */

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateAccessTokenResponse(newAccessToken));
    }
}
