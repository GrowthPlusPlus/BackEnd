// 정성우

package me.jsu.springbootdeveloper.oauth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/*
    구글 서버에서 OAuth과정을 승인하여 발급해준 토큰이 위조, 변조 되지 않았고 타당한지 해당 서버측에서 검증할 수 있도록 하는 클래스이다.
    OAuth 과정에 있어서 유저가 해당 서버를 사용하는데 필수적인 기능은 아니지만 구글이 사용하라고 권고하는 방식이다.
 */
@Service
public class GoogleIdTokenVerifyService {

    // Google ID Token의 암호학적 검증을 수행하는 핵심 객체로 JWT 서명, 발급자, 클라이언트 ID, 토큰 만료 시간을 검증한다.
    private final GoogleIdTokenVerifier verifier;

    /*
        해당 함수는 GoogleOAuthProperties의 값을 매개변수로 받아서 구글에서 받은 id 토큰을 검증용 객체에 주입하는 함수이다.
     */
    public GoogleIdTokenVerifyService(GoogleOAuthProperties props) {
        this.verifier = new GoogleIdTokenVerifier.Builder(  // 앞서 선언한 객체에 내용을 넣는다.
                new NetHttpTransport(),  // 구글 공개 키를 https로 가져오기 위한 함수이다.
                // Google API Client에서 사용하는 공식 JSON 파서이다.
                com.google.api.client.json.gson.GsonFactory.getDefaultInstance()
        )
                // id 토큰의 aud 클레임을 검증하는 부분으로, 이 토큰이 내 서버용으로 발급된 것인지 확인한다.
                .setAudience(List.of(
                        props.androidClientId(),  // 허용 목록으로 안드로이드, ios를 포함한다.
                        props.iosClientId()
                ))
                .build();  // 설정이 완료된 GoogleIdTokenVerifier를 생성한다.
    }

    /*
        해당 함수는 유저로부터 받은 id 토큰 문자열을 검증하기 위한 함수이다.
        생성자에서 얻은 구글 서버의 공개 키를 사용하여 jwt 서명을 검증한다.
     */
    public GoogleIdToken.Payload verify(String idToken) throws Exception {
        /*
            해당 verifier.verify에서 다음과 같은 사항을 검사하여 모두 만족하면 null 이외의 값을 token에 저장한다.
            1. 서명 유효
            2. 토큰 만료되지 않음
            3. issuer가 Google
            4. audience가 설정한 clientId와 일치
         */
        GoogleIdToken token = verifier.verify(idToken);
        if (token == null) {  // 검증을 실패하는 경우이다.
            throw new IllegalArgumentException("Invalid ID Token");
        }
        return token.getPayload();  // 검증에 성공하면 payload(claim)를 반환한다. 구글 고유 사용자 id, 사용자 이메일 등의 정보가 있다.
    }
}