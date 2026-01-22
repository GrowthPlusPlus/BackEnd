// 정성우

package me.jsu.springbootdeveloper.service.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/*
    클라이언트가 보낸 사진을 검증하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class ImageVerificationService {
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;  // 최대 이미지 크기이다.
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(  // 서버가 허용하는 이미지 유형이다.
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /*
        사용자가 업로드한 이미지를 검증하는 함수이다.
     */
    public void verify(MultipartFile image) {
        // 이미지가 없는 경우이다.
        if (image == null || image.isEmpty()) {
            throw new InvalidImageException("EMPTY_IMAGE");
        }

        validateFileSize(image);
        validateMimeType(image);
        validateImageDecoding(image);
        // verifyWithCLIP(image)  CLIP를 통한 AI 검사 방식으로 현재는 미구현이다.
    }

    /*
        사용자가 업로드 한 이미지가 과도하게 크진 않은지 검사한다.
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
}
