// 정성우

package me.jsu.springbootdeveloper.post.service;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.challenge.domain.Challenge;
import me.jsu.springbootdeveloper.challenge.service.ChallengeQueryService;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.post.dto.verify.ImageVerificationResponse;
import me.jsu.springbootdeveloper.post.event.CloudinaryDeleteEvent;
import me.jsu.springbootdeveloper.post.exception.InvalidImageException;
import me.jsu.springbootdeveloper.post.repository.PostPictureTemporaryRepository;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/*
    이미지를 임시로 업로드 할 수 있게 해주는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class ImageTemporaryService {
    private final CloudinaryService cloudinaryService;
    private final ImageVerificationService imageVerificationService;
    private final PostPictureTemporaryRepository postPictureTemporaryRepository;
    private final UserService userService;
    private final ChallengeQueryService challengeQueryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ImageVerificationResponse uploadTempImage(String email, Long challengeId, MultipartFile image) {
        User user = userService.findByEmail(email);
        Challenge challenge = challengeQueryService.findById(challengeId);

        // 이미지를 검증하고 CLIP 결과를 받는다.
        String passed = imageVerificationService.verify(challengeId, image);

        // 이미지를 클라우디너리에 업로드한다.
        PostPictureTemporary temp = saveImages(image, user, challenge);

        // CLIP 검증 결과와 임시로 업로드 한 이미지 id를 반환한다.
        return ImageVerificationResponse.builder()
                .passed(passed)
                .tempImageId(temp.getId())
                .build();
    }

    /*
        클라우디너리에 사진을 저장하고, 사진 정보를 임시로 DB에 반영해주는 함수이다.
     */
    public PostPictureTemporary saveImages(MultipartFile image, User user, Challenge challenge) {
        if (image != null) {
            // 인증글 이미지를 클라우디너리에 업로드하고 정보를 반환받는다.
            ImageUploadResult imageUploadResult = cloudinaryService.uploadImage(image);

            // 업로드한 사진을 임시로 저장한다.
            return postPictureTemporaryRepository.save(PostPictureTemporary.builder()
                    .user(user)
                    .challenge(challenge)
                    .cloudinaryPublicId(imageUploadResult.publicId())
                    .imageUrl(imageUploadResult.secureUrl())
                    .build());
        }
        else {  // 이미지가 없는 경우이지만 앞에서 검증을 하므로 방어코드이다.
            throw new InvalidImageException("이미지가 없습니다.");
        }
    }

    /*
        사용자가 일정 시간동안 임시 파일을 사용하지 않은 경우, 스케줄러가 호출하여 클라우디너리의 사진과 DB 정보를 삭제하는 함수이다.
     */
    public void deleteImages(PostPictureTemporary postPictureTemporary) {
        // 임시 사진들을 클라우디너리에서 삭제한다. 이미지가 없는 경우엔 if문을 실행하지 않는다.
        if (postPictureTemporary != null) {
            String publicId = postPictureTemporary.getCloudinaryPublicId();

            // 우리 서버의 DB에서 정보를 삭제한다.
            postPictureTemporaryRepository.delete(postPictureTemporary);

            // 클라우디너리의 이미지를 삭제한다. 트렌젝션이 끝난 후에 수행한다.
            applicationEventPublisher.publishEvent(new CloudinaryDeleteEvent(publicId));
        }
    }

    /*
        임시 파일의 DB 정보를 삭제하는 함수이다.
        인증글에 임시 파일이 사용된경우 임시파일은 삭제하는 용도이다.
     */
    public void deleteTempFile(PostPictureTemporary postPictureTemporary) {
        postPictureTemporaryRepository.delete(postPictureTemporary);
    }

    /*
        임시 파일 id들로 해당 id마다의 임시 파일 객체를 검색해주는 함수이다.
     */
    public List<PostPictureTemporary> findAllById(List<Long> ids) {
        return postPictureTemporaryRepository.findAllById(ids);
    }

    /*
        일정시간동안 사용하지 않은 임시 파일들을 검색해주는 함수이다.
     */
    public List<PostPictureTemporary> findExpired(Instant threshold) {
        return postPictureTemporaryRepository.findExpired(threshold);
    }
}
