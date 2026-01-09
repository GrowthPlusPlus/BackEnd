// 정성우

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

/*
    해당 클래스는 유효한 리프레시 토큰을 받아서 새로운 엑세스 토큰을 발급해주는 클래스이다.
    리프레시 토큰이 만료된 경우에는 사용되지 않는다.
 */
@RequiredArgsConstructor
@RestController
public class TokenApiController {
    private final TokenService tokenService;

    /*
        리프레시 토큰을 가지고 있는 상태에서 엑세스 토큰이 만료된 경우, 새로 발급하여 반환해주는 함수이다.
     */
    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(@RequestBody CreateAccessTokenRequest request) {
        String newAccessToken = tokenService.createNewAccessToken(request.getRefreshToken());  // 리프레시 토큰으로 엑세스 토큰을 생성한다.

        //ResponseEntity는 http 응답 전체를 직접 정의한다. 200 ok의 형태로 리턴한다.
        return ResponseEntity.ok(new CreateAccessTokenResponse(newAccessToken));
    }
}
