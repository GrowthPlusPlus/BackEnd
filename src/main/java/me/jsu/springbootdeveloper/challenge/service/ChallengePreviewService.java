// 정성우

package me.jsu.springbootdeveloper.challenge.service;

import me.jsu.springbootdeveloper.challenge.dto.preview.ChallengePreviewResponse;
import me.jsu.springbootdeveloper.challenge.dto.preview.ChallengePreviewResult;
import me.jsu.springbootdeveloper.post.exception.ClipServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/*
    챌린지 이름을 받아서 해당 챌린지가 사진 검증을 수행하기에 적합한지 판별하는 서비스이다.
 */
@Service
public class ChallengePreviewService {
    private static final Logger log = LoggerFactory.getLogger(ChallengePreviewService.class);
    private final WebClient webClient;

    // 의존성을 주입한다. config를 사용하면 default값이 주입되므로 직접 주입한다.
    public ChallengePreviewService(@Value("${clip.base-url}") String clipBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(clipBaseUrl)
                .build();
    }

    public ChallengePreviewResponse preview(String title) {
        try {
            // clip 서버에 검증 요청을 보내고 결과를 돌려받는다.
            ChallengePreviewResult result = webClient.post()
                    .uri("/preview")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("title", title))
                    .retrieve()
                    .bodyToMono(ChallengePreviewResult.class)
                    .block();

            if (result == null) {  // 응답이 비어있는 경우이다.
                log.error("CLIP returned empty response");
                throw new ClipServerException("CLIP server error");
            }

            return ChallengePreviewResponse.builder()
                    .autoVerifiable(result.autoVerifiable())
                    .build();

        } catch (Exception e) {  // 그 외의 예외 처리이다.
            log.error("CLIP verification failed", e);
            throw e;
        }
    }
}

