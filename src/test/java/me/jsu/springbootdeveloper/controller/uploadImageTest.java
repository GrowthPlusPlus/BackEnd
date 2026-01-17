// 정성우

package me.jsu.springbootdeveloper.controller;

import me.jsu.springbootdeveloper.dto.picture.ImageUploadResult;
import me.jsu.springbootdeveloper.service.cloudinary.CloudinaryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
    CloudinaryService의 이미지 업로드 기능을 테스트하는 통합 테스트 클래스이다.
    실제 클라우디너리 서비스에 이미지를 업로드하여 정상 동작 여부를 검증한다.
 */
@SpringBootTest
public class uploadImageTest {

    @Autowired
    private CloudinaryService cloudinaryService;

    @DisplayName("uploadImage: 이미지 업로드에 성공한다.")
    @Test
    void uploadImage_upload() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "tachibana.jpg",
                "image/jpeg",
                Files.readAllBytes(
                        Path.of("C:/Users/jsu29/바탕 화면/tachibana.jpg")
                )
        );

        // when
        ImageUploadResult imageUploadResult = cloudinaryService.uploadImage(file);

        // then
        assertNotNull(imageUploadResult);  // 업로드된 이미지 URL이 반환되는지 확인
    }

    @DisplayName("uploadImage: 이미지가 비어있어도 문제되지 않는다.")
    @Test
    void uploadImage_null() throws Exception {
        // given

        // when
        ImageUploadResult imageUploadResult = cloudinaryService.uploadImage(null);

        // then
        assertNull(imageUploadResult);  // 업로드 되지 않고 비어있는지 확인
    }
}
