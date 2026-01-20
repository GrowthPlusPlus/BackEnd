// 정성우

package me.jsu.springbootdeveloper.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.token.CreateAccessTokenRequest;
import me.jsu.springbootdeveloper.dto.token.TokenPair;
import me.jsu.springbootdeveloper.dto.token.TokenResponse;
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
        따라서 유저는 일정 기간 내에 앱을 사용하면 무한하게 로그인을 유지할 수 있다.
     */
    @Operation(
            summary = "Access Token 재발급",
            description = """
        만료된 Access Token 대신, 유효한 Refresh Token을 이용해 새로운 Access Token과 Refresh Token을 재발급한다.
        따라서 유저는 일정 기간 내에 앱을 사용하면 무한하게 로그인을 유지할 수 있다.

        Refresh Token이 유효하지 않거나 서버에 존재하지 않는 경우 인증에 실패한다.
        ex) POST /api/token
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 리프레시 토큰")
    })
    @PostMapping("/api/token")
    public ResponseEntity<TokenResponse> rotateRefreshToken(@RequestBody CreateAccessTokenRequest request) {
        TokenPair tokenPair = tokenService.rotateRefreshToken(request.getRefreshToken());  // 리프레시 토큰, 엑세스 토큰을 생성한다.

        // 새로 발급한 리프레시 토큰, 엑세스 토큰을 유저에게 반환한다.
        return ResponseEntity.ok(new TokenResponse(tokenPair.getAccessToken(), tokenPair.getRefreshToken()));
    }
}
