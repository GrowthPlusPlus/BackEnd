// 정성우

package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.CreateAccessTokenRequest;
import me.jsu.springbootdeveloper.dto.TokenPair;
import me.jsu.springbootdeveloper.dto.TokenResponse;
import me.jsu.springbootdeveloper.service.token.TokenService;
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
        또한 보안을 위해서 리프레시 토큰도 새로 발급한다.
        따라서 유저는 일정 기간내에 앱을 사용하면 무한하게 로그인을 유지할 수 있다.
     */
    @PostMapping("/api/token")
    public ResponseEntity<TokenResponse> rotateRefreshToken(@RequestBody CreateAccessTokenRequest request) {
        TokenPair tokenPair = tokenService.rotateRefreshToken(request.getRefreshToken());  // 리프레시 토큰, 엑세스 토큰을 생성한다.

        // 새로 발급한 리프레시 토큰, 엑세스 토큰을 유저에게 반환한다.
        return ResponseEntity.ok(new TokenResponse(tokenPair.getAccessToken(), tokenPair.getRefreshToken()));
    }
}
