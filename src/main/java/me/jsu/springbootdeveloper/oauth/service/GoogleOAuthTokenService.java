// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import me.jsu.springbootdeveloper.oauth.dto.GoogleTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/*
    OAuth의 Token Exchange 단계를 담당하는 클래스이다.
    ==========
    OAuth + PKCE 전체 흐름 중에서 해당 위치이다.
    (앱) Google 로그인 → authorization code 획득
    (서버) authorization code → Google 토큰 교환 ← 이 클래스. 즉 클라이언트로부터 받은 code를 구글 서버에 준다.
    (서버) id_token 검증
    (서버) 우리 서비스 JWT 발급
    즉, 로그인도 아니고 JWT 발급도 아니며, Google OAuth 프로토콜 통신을 담당한다.
 */
@Service
public class GoogleOAuthTokenService {
    // Google Token Endpoint인 밑의 uri를 호출하기 위한 객체이다.
    private final WebClient webClient = WebClient.builder().build();
    // application.yml에 정의된 Google OAuth 설정값으로 client_id, client_secret, redirect_uri를 제공한다.
    private final GoogleOAuthProperties props;

    // GoogleOAuthProperties 클래스의 내용을 주입받는다.
    public GoogleOAuthTokenService(GoogleOAuthProperties props) {
        this.props = props;
    }

    /*
        해당 함수는 PKCE를 이용한 OAuth 인증 과정에서 사용되는 함수이다.

        1. 서버가 Google Token Endpoint에 요청 전송
        2. code + code_verifier를 Google이 검증(PKCE)
        3. 성공하면 Google이 토큰 JSON 반환
        4. 그 JSON을 GoogleTokenResponse로 변환해서 리턴
        위의 과정을 수행한다.

        해당 클래스의 매개변수로는 다음을 갖는다.
        code: 구글 Authorization Server가 발급한 authorization code로 1회성이며, 짧은 수명을 가진다.
        codeVerifier: PKCE에서 사용하는 값으로 앱이 처음에 생성한 값이다. Google이 내부에 저장해둔 code_challenge와 비교한다.
     */
    public GoogleTokenResponse exchangeCode(String code, String codeVerifier, String clientId) {
        // HTTP의 POST 함수이다. token endpoint는 POST로 호출한다.
        return webClient.post()
                // Google이 제공하는 OAuth Token Endpoint로 구글 서버와 내 서버가 소통하는 API이다.
                .uri("https://oauth2.googleapis.com/token")
                // 요청 바디 타입을 application/x-www-form-urlencoded로 설정한다.
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // 폼 파라미터 문자열을 만들어 구글에 POST 요청을 보내는 형태이다.
                // URL 인코딩을 스프링이 책임질 수 있도록 BodyInserters.fromFormData()를 사용한다.
                .body(
                        BodyInserters.fromFormData("grant_type", "authorization_code")  // Authorization Code Flow로 토큰을 교환하겠다.
                                .with("client_id", clientId)  // Google에 등록된 앱/프로젝트를 식별하는 값이다. 안드로이드, ios를 구별한다.
                                // Google 로그인 요청 때 사용한 redirectUri와 동일해야 한다. 요청의 일관성을 위해 검사하는 값이다.
                                .with("redirect_uri", props.redirectUri())
                                .with("code", code)  // Google이 로그인/동의 후 발급해 준 1회성 코드이다.
                                /*
                                    앱이 처음 생성했던 PKCE 비밀값이다.
                                    Google은 이 값을 받아서 SHA256(code_verifier)를 계산한 후에 처음에 받았던 code_challenge와 비교한다.
                                 */
                                .with("code_verifier", codeVerifier)
                )  // 전체 과정에서 내 서버는 전달만 하고 검증은 구글이 수행한다.
                .retrieve()  // http 응답을 가져오겠다는 의미이다.
                .bodyToMono(GoogleTokenResponse.class)  // 응답 바디(JSON)를 GoogleTokenResponse 타입으로 역직렬화 한다.
                .block();  // 이 함수는 호출자에게 GoogleTokenResponse를 바로 반환한다.
    }
}
