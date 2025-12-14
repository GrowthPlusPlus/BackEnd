package me.jsu.springbootdeveloper.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.util.Base64;

/*
HttpServletRequest
"클라이언트가 서버에게 보낸 요청 정보"를 담은 객체

● Request 내부에 담긴 대표적인 값들
요청 URL
HTTP 메서드(GET, POST 등)
요청 헤더
쿠키
쿼리 파라미터( ?뒤에 있는 값 )
요청 바디(JSON·Form 데이터 등)
사용자 IP 정보

HttpServletResponse
"서버가 클라이언트에게 돌려주는 응답을 만들기 위한 객체"

● Response로 할 수 있는 일들
응답 본문(body) 쓰기
응답 상태코드 설정 (200, 404, 500 등)
응답 헤더 설정
쿠키 추가/삭제
리다이렉트 수행 (response.sendRedirect)

● 쉽게 말하면
👉 "서버가 사용자에게 어떤 내용을 보낼지 조립하는 도구"
 */

public class CookieUtil {
    // 요청값을 바탕으로 쿠기 추가
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);  // 쿠키 객체 생성
        cookie.setPath("/");  // 앱 전체에서 쿠키가 전송되도록 경로 설정
        cookie.setMaxAge(maxAge);  // 쿠키의 유효 시간
        response.addCookie(cookie);  // http 응답 헤더에 쿠키 추가
    }

    // 쿠키의 이름을 입력받아 삭제
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);  // 만료시간을 0으로 만들어 쿠키를 의미없게 만듦
                response.addCookie(cookie);
            }
        }
    }

    // 객체를 직렬화해 쿠키의 값으로 변환
    public static String serialize(Object obj) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(obj));  //변환된 obj값을 쿠키값으로 사용
    }

    // 쿠키를 역직렬화해 객체로 반환
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue())));  // 쿠키 데이터를 원래 객체로 복원
    }
}
