// 정성우

package me.jsu.springbootdeveloper.config.resend;

/*
    회원가입 시 이메일 인증을 위해 필요한 설정이다.
 */
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
        String apiKey,
        String from
) {}
