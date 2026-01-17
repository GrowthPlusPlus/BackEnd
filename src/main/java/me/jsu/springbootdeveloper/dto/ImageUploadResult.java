// 정성우

package me.jsu.springbootdeveloper.dto;

/*
    이미지 업로드에 대한 결과값을 반환하는 dto이다.
    이미지에 접근할 주소, 이미지를 관리할 id 정보를 가지고 있다.
 */
public record ImageUploadResult(
        String secureUrl,
        String publicId
) {}
