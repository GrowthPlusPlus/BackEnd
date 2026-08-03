// 정성우

package me.jsu.springbootdeveloper.post.service;

import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.post.dto.verify.ImageVerificationResult;
import me.jsu.springbootdeveloper.post.exception.ClipServerException;
import me.jsu.springbootdeveloper.post.exception.InvalidImageException;
import me.jsu.springbootdeveloper.post.repository.PostPictureTemporaryRepository;
import me.jsu.springbootdeveloper.tag.domain.TagToChallenge;
import me.jsu.springbootdeveloper.tag.service.TagService;
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
import java.util.List;
import java.util.Set;
import java.net.URL;

/*
    클라이언트가 보낸 사진을 검증하는 서비스이다.
 */
@Service
public class ImageVerificationService {
    private static final Logger log = LoggerFactory.getLogger(ImageVerificationService.class);

    private final WebClient webClient;
    private final ChallengeQueryService challengeQueryService;
    private final TagService tagService;
    private final PostPictureTemporaryRepository postPictureTemporaryRepository;

    // 의존성을 주입한다. config를 사용하면 default값이 주입되므로 직접 주입한다.
    public ImageVerificationService(
            @Value("${clip.base-url}") String clipBaseUrl,
            ChallengeQueryService challengeQueryService,
            TagService tagService,
            PostPictureTemporaryRepository postPictureTemporaryRepository
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(clipBaseUrl)
                .build();
        this.challengeQueryService = challengeQueryService;
        this.tagService = tagService;
        this.postPictureTemporaryRepository = postPictureTemporaryRepository;
    }

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;  // 최대 이미지 크기이다.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(  // 서버가 허용하는 이미지 유형이다.
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /*
        사용자가 업로드한 이미지를 검증하는 함수이다. 문제가 없는지 검증한다.
     */
    public void verify(MultipartFile image) {
        // 이미지가 없는 경우이다.
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException("EMPTY_IMAGE");
        }

        // 이미지에 대해 각종 검증을 한다.
        validateFileSize(image);
        validateMimeType(image);
        validateImageDecoding(image);
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
    public ImageVerificationResult verifyWithCLIP(Long challengeId, Long temporaryImageId) {
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 임시 이미지의 db값을 조회한다.
        PostPictureTemporary postPictureTemporary = postPictureTemporaryRepository.findById(temporaryImageId)
                .orElseThrow(() -> new InvalidImageException("CANNOT_FIND_TEMPORARY_IMAGE"));

        // 보낼 값을 map으로 만든다.
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", buildClipPrompt(challenge));

        try {
            // Cloudinary URL에서 이미지를 다운로드한다.
            URL imageUrl = new URL(postPictureTemporary.getImageUrl());

            try (InputStream inputStream = imageUrl.openStream()) {
                byte[] imageBytes = inputStream.readAllBytes();

                // URL에서 원본 파일명을 추출한다.
                String imageUrlString = postPictureTemporary.getImageUrl();
                final String fileName =
                        imageUrlString.substring(imageUrlString.lastIndexOf('/') + 1)
                                .split("\\?")[0];

                // 다운로드한 이미지를 multipart 파일처럼 만들어 CLIP 서버에 전송한다.
                body.add("image", new ByteArrayResource(imageBytes) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                });
            }
        }
        catch (IOException e) {
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

            return result;

        } catch (Exception e) {  // 그 외의 예외 처리이다.
            log.error("CLIP verification failed", e);
            throw e;
        }
    }

    /*
        챌린지 이름 뒤에 태그를 붙이기 위한 함수이다.
        clip 서버에 이름 뿐만 아니라 태그 정보도 보내게 된다.
     */
    private String buildClipPrompt(Challenge challenge) {
        StringBuilder prompt = new StringBuilder(challenge.getTitle());

        // 챌린지의 태그들을 조회한다.
        List<TagToChallenge> tags = tagService.searchChallengeTags(challenge.getId());

        // 최대 3개까지만 태그를 사용한다.
        int count = 0;
        for (TagToChallenge tagToChallenge : tags) {
            if (count >= 3) {
                break;
            }

            prompt.append(", ").append(tagToChallenge.getTag().getTagName());
            count++;
        }

        return prompt.toString();
    }
}
