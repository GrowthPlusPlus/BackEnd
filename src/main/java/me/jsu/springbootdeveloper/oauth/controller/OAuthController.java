// 정성우

package me.jsu.springbootdeveloper.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.oauth.dto.GoogleCodeExchangeRequest;
import me.jsu.springbootdeveloper.oauth.dto.KakaoCodeExchangeRequest;
import me.jsu.springbootdeveloper.oauth.dto.LoginResponse;
import me.jsu.springbootdeveloper.oauth.dto.NaverCodeExchangeRequest;
import me.jsu.springbootdeveloper.oauth.service.OAuthGoogleService;
import me.jsu.springbootdeveloper.oauth.service.OAuthKakaoService;
import me.jsu.springbootdeveloper.oauth.service.OAuthNaverService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/*
    해당 클래스는 모바일 앱에서 들어오는 Google OAuth 로그인 요청의 진입점(Controller) 역할을 한다.
    즉 모바일 앱으로부터 Google OAuth 인증 코드를 받아 로그인 유스케이스(Service)를 호출하고
    그 결과를 HTTP 응답으로 반환하는 웹 계층 컴포넌트이다.
 */
@RestController  // 반환 값을 json으로 자동 직렬화 한다.
@RequiredArgsConstructor  // final 필드를 생성자로 주입한다.
public class OAuthController {
    private final OAuthGoogleService oauthGoogleService;
    private final OAuthKakaoService oauthKakaoService;
    private final OAuthNaverService oauthNaverService;

    /*
        이 함수는 OAuth + PKCE 로그인 흐름의 HTTP 엔드포인트이다.
        앱 → authorization code + code_verifier
            ↓
        서버 (/api/oauth/google/token) ← 이 메서드
            ↓
        OAuthLoginService 호출

        즉 모바일에서 받은 값을 우리 서버가 구글 서버에 보내는 과정이다.

        함수의 @RequestBody 파라미터는 모바일 앱이 전달한 json 요청 바디로, code (인증 코드), codeVerifier (PKCE 검증용 값)가 들어있다.
     */
    @Operation(
            summary = "Google OAuth 로그인 (PKCE)",
            description = """
        모바일 앱에서 전달된 Google OAuth authorization code와 PKCE code_verifier를 이용하여 로그인 처리한다.
        위 값은 모바일 -> 구글 서버로 거치면서서 만든 값이다.
        해당 값을 우리 서버에서 받아 값을 만들어준 구글 서버와 다시 비교하는 작업을 거친다.
        따라서 클라이언트는 GoogleCodeExchangeRequest dto를 보내야 한다.
        
        로그인 성공 시 Access Token과 Refresh Token, 최초 로그인 사용자 인지에 대한 여부를 반환한다.
        최초 사용자라면 회원 초기 설정으로 화면을 띄우는데 도움을 준다.
        ex) POST /api/oauth/google/token
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OAuth 로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "OAuth 로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/api/oauth/google/token")
    public ResponseEntity<LoginResponse> loginGoogle(@RequestBody GoogleCodeExchangeRequest request) {
        /*
            실제 처리를 위해 service 계층을 호출한다.
            서비스 계층에서 로그인에 성공하면 response에 엑세스 토큰, 리프레스 토큰을 반환한다. 이 값이 json 응답이다.
            예외 처리는 모두 서비스 계층에 위임한다.
         */
        LoginResponse response = oauthGoogleService.loginWithGoogle(
                request.getCode(),
                request.getCodeVerifier(),
                request.getClientId(),
                request.getFcmToken()  // FCM 토큰이 있으면 로그인 시 자동으로 등록된다.
        );

        return ResponseEntity.ok(response);  // 200 OK + json 응답을 반환한다.
    }

    @Operation(
            summary = "Kakao OAuth 로그인 (PKCE)",
            description = """
        모바일 앱에서 전달된 Kakao OAuth authorization code와 PKCE code_verifier를 이용하여 로그인 처리한다.
        위 값은 모바일 -> 카카오 서버로 거치면서서 만든 값이다.
        해당 값을 우리 서버에서 받아 값을 만들어준 카카오 서버와 다시 비교하는 작업을 거친다.
        따라서 클라이언트는 KakaoCodeExchangeRequest dto를 보내야 한다.
        
        로그인 성공 시 Access Token과 Refresh Token, 최초 로그인 사용자 인지에 대한 여부를 반환한다.
        최초 사용자라면 회원 초기 설정으로 화면을 띄우는데 도움을 준다.
        ex) POST /api/oauth/kakao/token
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OAuth 로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "OAuth 로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/api/oauth/kakao/token")
    public ResponseEntity<LoginResponse> loginKakao(@RequestBody KakaoCodeExchangeRequest request) {
        LoginResponse response = oauthKakaoService.loginWithKakao(
                        request.getCode(),
                        request.getCodeVerifier(),
                        request.getFcmToken()
        );

        return ResponseEntity.ok(response);
    }

    /*
        리다이렉트를 위한 컨트롤러이다.
     */
    @GetMapping("/oauth/kakao/callback")
    public void kakaoCallback(@RequestParam String code, @RequestParam(required = false) String state, HttpServletResponse response) throws IOException {
        response.sendRedirect("kakao://oauth?code=" + code + "&state=" + state);
    }

    @Operation(
            summary = "Naver OAuth 로그인",
            description = """
        모바일 앱에서 전달된 Naver OAuth authorization code와 State를 이용하여 로그인 처리한다.
        구글, 카카오와는 다르게 code_verifier를 네이버는 사용하지 않고 State값만 사용한다.
        위 값은 모바일 -> 네이버 서버로 거치면서서 만든 값이다.
        해당 값을 우리 서버에서 받아 값을 만들어준 네이버 서버와 다시 비교하는 작업을 거친다.
        따라서 클라이언트는 NaverCodeExchangeRequest dto를 보내야 한다.
        
        로그인 성공 시 Access Token과 Refresh Token, 최초 로그인 사용자 인지에 대한 여부를 반환한다.
        최초 사용자라면 회원 초기 설정으로 화면을 띄우는데 도움을 준다.
        ex) POST /api/oauth/Naver/token
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OAuth 로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "OAuth 로그인 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/api/oauth/naver/token")
    public ResponseEntity<LoginResponse> loginNaver(@RequestBody NaverCodeExchangeRequest request) {
        LoginResponse response = oauthNaverService.loginWithNaver(
                request.getCode(),
                request.getState(),
                request.getFcmToken()
        );

        return ResponseEntity.ok(response);
    }
}