package me.jsu.springbootdeveloper;

import me.jsu.springbootdeveloper.config.oauth.GoogleOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing  // created, updated를 자동 업데이트
@EnableConfigurationProperties(GoogleOAuthProperties.class)
@SpringBootApplication
public class SpringBootDeveloperApplication {
        public static void main(String[] args) {
            SpringApplication.run(SpringBootDeveloperApplication.class, args);
        }
    }