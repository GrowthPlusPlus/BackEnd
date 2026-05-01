// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.post.dto.verify.ImageVerificationResult;
import me.jsu.springbootdeveloper.post.exception.ClipServerException;
import me.jsu.springbootdeveloper.post.exception.InvalidImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/*
    클라이언트가 보낸 사진을 검증하는 서비스이다.
 */
@Service
public class ImageVerificationService {
    private static final Logger log = LoggerFactory.getLogger(ImageVerificationService.class);

    private final WebClient webClient;
    private final ChallengeQueryService challengeQueryService;

    // 의존성을 주입한다. config를 사용하면 default값이 주입되므로 직접 주입한다.
    public ImageVerificationService(
            @Value("${clip.base-url}") String clipBaseUrl,
            ChallengeQueryService challengeQueryService
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(clipBaseUrl)
                .build();
        this.challengeQueryService = challengeQueryService;
    }

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;  // 최대 이미지 크기이다.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(  // 서버가 허용하는 이미지 유형이다.
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /*
        사용자가 업로드한 이미지를 검증하는 함수이다.
     */
    public String verify(Long challengeId, MultipartFile image) {
        // 이미지가 없는 경우이다.
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException("EMPTY_IMAGE");
        }

        // 이미지에 대해 각종 검증을 한다.
        validateFileSize(image);
        validateMimeType(image);
        validateImageDecoding(image);

        return verifyWithCLIP(challengeId, image);  // CLIP 결과를 프론트에 반환한다.
        // CLIP 방식이 변경될 수 있으므로 일단 보류함
        //return "PASS";
    }

    /*
        사용자가 업로드 한 이미지가 과도하게 크진 않은지 검사한다.s
     */
    private void validateFileSize(MultipartFile image) {
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException("TOO_BIG_SIZE_IMAGE");
        }
    }

    /*
        사용자가 업로드 한 이미지가 허용되지 않은 이미지 포멧인지 검사한다.
     */
    private void validateMimeType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new InvalidImageException("UNSUPPORTED_IMAGE_FORMAT");
        }
    }

    /*
        사용자가 업로드 한 이미지가 진짜 이미지인지 검사한다.
        디코딩이 불가능하거나 손상된 이미지인지 확인한다.
     */
    private void validateImageDecoding(MultipartFile image) {
        try (InputStream is = image.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {  // 손상된 이미지이다.
                throw new InvalidImageException("CORRUPTED_IMAGE_FILE");
            }
        } catch (IOException e) {  // 이미지 디코딩이 실패한 경우이다.
            throw new InvalidImageException("IMAGE_DECODING_FAILED");
        }
    }

    /*
        clip 서버에 챌린지 제목과 이미지를 보내 검증 결과를 얻는 함수이다.
     */
    public String verifyWithCLIP(Long challengeId, MultipartFile image) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 보낼 값을 map으로 만든다.
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", challenge.getTitle() );
        try {
            body.add("image", new ByteArrayResource(image.getBytes()) {  // 파일명을 명시적으로 지정한다.
                @Override
                public String getFilename() {
                    return image.getOriginalFilename();
                }
            });
        } catch (IOException e) {
            throw new ClipServerException("Failed to read image bytes");
        }

        try {
            // clip 서버에 검증 요청을 보내고 결과를 돌려받는다.
            ImageVerificationResult result = webClient.post()
                    .uri("/verify")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(ImageVerificationResult.class)
                    .block();

            if (result == null) {  // 응답이 비어있는 경우이다.
                log.error("CLIP returned empty response");
                throw new ClipServerException("CLIP server error");
            }

            return result.passed();

        } catch (Exception e) {  // 그 외의 예외 처리이다.
            log.error("CLIP verification failed", e);
            throw e;
        }
    }
}
