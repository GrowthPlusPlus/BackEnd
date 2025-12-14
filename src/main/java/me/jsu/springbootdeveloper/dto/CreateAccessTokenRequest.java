package me.jsu.springbootdeveloper.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccessTokenRequest {  // 엑세스 토큰을 만들기 위해 리프레시 토큰에 대한 정보만을 가진 request
    private String refreshToken;
}
