// 정성우

package me.jsu.springbootdeveloper.dto.oauth;

/*
    이 클래스의 코드는 모바일 앱이 Google OAuth(PKCE) 인증을 완료한 뒤, 우리 서버에 전달하는 요청 데이터의 구조를 정의한 DTO이다.
    즉 모바일 앱 -> 우리 서버 -> 구글 서버 단계에서 앱에서 우리 서버로 요청 데이터를 보낼때 쓰는 dto이다.
    이 요청 데이터를 구글 서버로 보내면, 구글 서버는 값을 검증한다.

    code: 구글 Authorization Server가 발급한 authorization code로 1회성이며, 짧은 수명을 가진다.
    codeVerifier: PKCE에서 사용하는 값으로 앱이 처음에 생성한 값이다. Google이 내부에 저장해둔 code_challenge와 비교한다.
 */
public record GoogleCodeExchangeRequest(
        String code,
        String codeVerifier
) {}
