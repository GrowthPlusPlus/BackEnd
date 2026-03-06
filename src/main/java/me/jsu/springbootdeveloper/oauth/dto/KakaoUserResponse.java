// 정성우

package me.jsu.springbootdeveloper.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/*
    인증한 유저의 정보가 담긴 dto로 카카오톡에선 구글과 달리 id_token을 사용하지 않으므로 직접 받는다.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true) // 예상치 못한 필드 무시
public class KakaoUserResponse {
    // 카카오 고유 사용자 ID (항상 존재)
    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    /*
        안전한 이메일 접근
     */
    public String getEmailSafe() {
        if (kakaoAccount == null) {
            return null;
        }
        if (Boolean.TRUE.equals(kakaoAccount.emailNeedsAgreement)) {
            return null;
        }

        return kakaoAccount.email;
    }

    /*
        안전한 닉네임 접근
     */
    public String getNicknameSafe() {
        if (kakaoAccount == null) {
            return "kakao_user";
        }
        if (kakaoAccount.profile == null) {
            return "kakao_user";
        }
        if (kakaoAccount.profile.nickname == null) {
            return "kakao_user";
        }

        return kakaoAccount.profile.nickname;
    }

    /*
        내부 DTO (파일 분리 안함)
     */
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {
        private String email;

        @JsonProperty("email_needs_agreement")
        private Boolean emailNeedsAgreement;

        @JsonProperty("is_email_valid")
        private Boolean isEmailValid;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        private Profile profile;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        @JsonProperty("thumbnail_image_url")
        private String thumbnailImageUrl;
    }
}