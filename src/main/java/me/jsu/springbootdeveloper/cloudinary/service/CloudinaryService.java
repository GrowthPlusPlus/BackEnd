// 정성우

package me.jsu.springbootdeveloper.cloudinary.service;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.cloudinary.exception.CloudinaryDeleteException;
import me.jsu.springbootdeveloper.cloudinary.exception.CloudinaryUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/*
    실제로 사진을 받아서 클라우디너리에 업로드 하는 클래스이다.
    컨트롤러에서 @RequestPart(required = false) MultipartFile image 형태로 매개변수를 받으면, image 파일을 밑의 함수에 주어서 업로드한다.
    우리 서비스의 DB에 저장하는 부분은 다른 서비스에서 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    /*
        사진을 받아서 비어있으면 아무런 일을 하지 않고, 업로드에 실패하면 예외처리를 한다.
     */
    public ImageUploadResult uploadImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return null;  // 사진이 없으면 null을 반환한다.
            }

            // 클라이언트가 보낸 사진을 클라우디너리에 업로드 하고 결과를 map으로 받는다.
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), Map.of());

            Object secureUrl = uploadResult.get("secure_url");  // 사진의 주소를 추출한다.
            Object publicId = uploadResult.get("public_id");  // 사진을 관리하기 위한 id를 추출한다.
            if (secureUrl == null || publicId == null) {  // null이면 사진을 업로드하지 못한 것이다.
                throw new CloudinaryUploadException("PICTURE_IS_NOT_UPLOADED");
            }

            return new ImageUploadResult(secureUrl.toString(), publicId.toString());  // 사진 정보를 반환한다.
        } catch (IOException e) {  // 전체 과정의 오류이다.
            throw new CloudinaryUploadException("CLOUDINARY_UPLOAD_ERROR");
        }
    }

    /*
        클라우디너리의 사진을 id로 삭제한다.
     */
    public void deleteImage(String publicId) {
        try {
            Map destroyResult = cloudinary.uploader().destroy(publicId, Map.of());  // result에 ok 또는 not found 값이 존재한다.
            Object result = destroyResult.get("result");  // result 값을 추출한다.

            if ("not found".equals(result)) {  // 없는 이미지를 삭제하려하면 그냥 반환한다.
                return;
            }

            if (!"ok".equals(result)) {  // 잘 삭제됐는지 검사하고 문제가 있다면 예외처리한다.
                throw new CloudinaryDeleteException("PICTURE_IS_NOT_DELETED");
            }
        } catch (IOException e) {  // 전체 과정의 오류이다.
            throw new CloudinaryDeleteException("CLOUDINARY_DELETE_ERROR");
        }
    }
}
