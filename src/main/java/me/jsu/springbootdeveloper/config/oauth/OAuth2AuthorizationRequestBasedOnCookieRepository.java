// 쿠키 기반 OAUTH 설정

//package me.jsu.springbootdeveloper.config.oauth;
//
//import jakarta.servlet.http.Cookie;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import me.jsu.springbootdeveloper.util.CookieUtil;
//import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.web.util.WebUtils;
//
//public class OAuth2AuthorizationRequestBasedOnCookieRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
//
//    public final static String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
//    private final static int COOKIE_EXPIRE_SECONDS = 300;  // 5분동안 쿠키를 유지한다.
//
//    // 로그인 완료 후 authorizationRequest 쿠키를 제거하기 위해 삭제해야하는 authorizationRequest가 무엇인지 반환하는 역할을 하는 메서드임
//    @Override
//    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
//        return this.loadAuthorizationRequest(request);
//    }
//
//    // request에 들어있는 쿠키를 읽고 역직렬화하여 객체로 복원함 -> 구글 서버에서 받은 쿠키화된 authorizationRequest값을 객체로 바꾸어서 인증에 사용함
//    @Override
//    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
//        Cookie cookie = WebUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
//        return CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class);
//    }
//
//    // 구글 서버에 요청하여 받은 authorizationRequest값을 직렬화 하여 쿠키로 저장함
//    @Override
//    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
//        if (authorizationRequest == null) {  // 구글 인증이 완료되면 쿠키를 삭제함
//            removeAuthorizationRequest(request, response);
//            return;
//        }
//        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
//    }
//
//    // 실제로 authorizationRequest 쿠키를 삭제하는 메서드
//    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
//        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
//    }
//}
