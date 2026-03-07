// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/*
    인증한 유저의 정보가 담긴 dto로 네이버에선 구글과 달리 id_token을 사용하지 않으므로 직접 받는다.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverUserResponse {
    @JsonProperty("resultcode")
    private String resultCode;
    private String message;
    private Response response;

    public String getEmailSafe() {
        if (response == null || response.email == null) {
            return null;
        }
        return response.email;
    }

    public String getNicknameSafe() {
        if (response == null || response.nickname == null || response.nickname.isBlank()) {
            return "naver_user";
        }
        return response.nickname;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private String id;

        @JsonProperty("email")
        private String email;

        @JsonProperty("nickname")
        private String nickname;  // 네이버에서 사용하는 별명

        @JsonProperty("name")
        private String name;  // 실명
    }
}
