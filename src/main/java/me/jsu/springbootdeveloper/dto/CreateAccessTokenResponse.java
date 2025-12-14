package me.jsu.springbootdeveloper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateAccessTokenResponse {  // 만들어진 엑세스 토큰을 response로 담은 클래스
    private String accessToken;
}
