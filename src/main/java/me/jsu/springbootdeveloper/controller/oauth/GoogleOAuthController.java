// 정성우

package me.jsu.springbootdeveloper.controller.oauth;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.dto.oauth.GoogleCodeExchangeRequest;
import me.jsu.springbootdeveloper.dto.oauth.LoginResponse;
import me.jsu.springbootdeveloper.service.oauth.OAuthLoginService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/oauth/google")
@RequiredArgsConstructor
public class GoogleOAuthController {

    private final OAuthLoginService oauthLoginService;

    @PostMapping("/token")
    public ResponseEntity<LoginResponse> login(
            @RequestBody GoogleCodeExchangeRequest request
    ) {
        try {
            LoginResponse response =
                    oauthLoginService.loginWithGoogle(
                            request.code(),
                            request.codeVerifier()
                    );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {  // 예외 처리 개선해야 함
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}