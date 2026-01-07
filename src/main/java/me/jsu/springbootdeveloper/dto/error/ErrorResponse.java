// 정성우

package me.jsu.springbootdeveloper.dto.error;

/*
    해당 클래스의 코드는 클라이언트(모바일 앱)에 전달할 JSON 응답 형식을 정의한 DTO이다.
    record를 사용하여 해당 dto는 단순 데이터 전달, 불변, 생성 후 변경 불가 등의 특징이 있다.
 */
public record ErrorResponse(
        String error,  // 실패 유형을 나타낸다.
        String reason  // 구체적인 실패 이유를 나타낸다.
) {}