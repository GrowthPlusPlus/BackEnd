package me.jsu.springbootdeveloper;

import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import me.jsu.springbootdeveloper.config.oauth.KakaoOAuthProperties;
import me.jsu.springbootdeveloper.config.oauth.NaverOAuthProperties;
import me.jsu.springbootdeveloper.config.resend.ResendProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling  // 스케줄러가 동작하도록 한다.
@EnableJpaAuditing  // created, updated를 자동 업데이트
@EnableConfigurationProperties({GoogleOAuthProperties.class, KakaoOAuthProperties.class, NaverOAuthProperties.class, ResendProperties.class})
@EnableRetry  // Spring Retry 기능을 활성화한다.
@SpringBootApplication
public class SpringBootDeveloperApplication {
        public static void main(String[] args) {
            SpringApplication.run(SpringBootDeveloperApplication.class, args);
        }
    }