// 여하늘

package me.jsu.springbootdeveloper.config.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

/*
    Firebase Cloud Messaging(FCM)을 초기화하는 클래스이다.
    애플리케이션 시작 시 Firebase Admin SDK를 초기화하여 푸시 알림을 보낼 수 있도록 설정한다.
 */
@Component
public class FCMInitializer {
    /*
        애플리케이션 시작 시 Firebase Admin SDK를 초기화한다.
        Firebase 서비스 계정 키 파일을 읽어서 FirebaseApp을 초기화한다.
     */
    @Value("${fcm.firebase_config_path}")
    private String firebaseConfigPath;

    @PostConstruct
    public void initialize() {
        // Firebase 서비스 계정 키 파일 경로를 지정하고 서비스 계정 키 파일을 읽어온다.
        try (InputStream serviceAccount = new FileInputStream(firebaseConfigPath)) {

            // Firebase 옵션을 설정한다.
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))  // 서비스 계정 키로 인증 정보를 설정한다.
                    .build();

            // FirebaseApp이 이미 초기화되어 있지 않은 경우에만 초기화한다.
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);  // Firebase Admin SDK를 초기화한다.
                System.out.println("Firebase Admin SDK 초기화 완료");
            } else {
                System.out.println("Firebase Admin SDK가 이미 초기화되어 있습니다.");
            }
        } catch (IOException e) {
            // Firebase 서비스 계정 키 파일을 읽는 중 오류가 발생한 경우 예외를 던진다.
            throw new RuntimeException("Firebase 초기화 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
