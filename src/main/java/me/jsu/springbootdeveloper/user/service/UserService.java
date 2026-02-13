// 정성우

package me.jsu.springbootdeveloper.user.service;

import me.jsu.springbootdeveloper.user.domain.UserStatistics;
import me.jsu.springbootdeveloper.user.dto.MyProfileResponse;
import me.jsu.springbootdeveloper.user.exception.DuplicateNicknameException;
import me.jsu.springbootdeveloper.user.exception.UserNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.user.enumType.UserStatus;
import me.jsu.springbootdeveloper.user.dto.AddUserRequest;
import me.jsu.springbootdeveloper.cloudinary.dto.ImageUploadResult;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.cloudinary.exception.CloudinaryUploadException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static me.jsu.springbootdeveloper.user.enumType.Role.ROLE_USER;

/*
    해당 클래스는 유저 데이터베이스를 이용하여 검색하거나 데이터베이스에 엔트리를 저장하는 역할을 한다.
 */
@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfilePictureRepository userProfilePictureRepository;
    private final CloudinaryService cloudinaryService;
    private final UserStatisticsRepository userStatisticsRepository;

    /*
        회원가입을 구현한 함수이다. 현재는 OAuth 인증 방식을 사용하므로 사용되지 않는다.

        @param dto 회원가입 요청 정보
        @return 생성된 유저의 id
     */
    public Long save(AddUserRequest dto) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        return userRepository.save(User.builder()
                .email(dto.getEmail())
                .password(encoder.encode(dto.getPassword()))  // 패스워드를 암호화한다.
                .role(ROLE_USER)
                .build()).getId();  // 데베에 등록된 id를 리턴값으로 반환한다.
    }

    /*
        유저 id를 기반으로 유저 정보를 검색하는 함수이다.
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_ID"));
    }

    /*
        유저 이메일을 기반으로 유저 정보를 검색하는 함수이다.
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_EMAIL"));
    }

    /*
        이메일로 유저를 찾거나 없으면 생성하는 함수이다.
        PKCE OAuth2 인증에서 사용자 정보를 처리하기 위해 사용한다.
        기존 유저가 있으면 닉네임을 업데이트하고, 없으면 새로 생성한다.

        @param email 유저 이메일
        @param name 유저 닉네임
        @return 찾거나 생성된 User 엔티티
     */
    public User findOrCreate(String email, String name) {
        User user = userRepository.findByEmail(email)
                // .map(user -> user) 기존 유저가 존재하는 경우이다. 여기선 사용하지 않으므로 주석처리한다.
                .orElseGet(() ->  // 신규 유저인 경우이다.
                        userRepository.save(
                                User.builder()
                                        .email(email)
                                        .nickname(name)
                                        .role(ROLE_USER)  // 신규 유저는 일반 유저로 권한을 부여한다.
                                        .build()
                        )
                );

        // 유저의 통계 DB를 생성한다.
        UserStatistics userStats = userStatisticsRepository.findByUser_Id(user.getId())
                .orElseGet(() -> userStatisticsRepository.save(UserStatistics.builder()
                        .user(user)
                        .build()));

        return user;
    }

    /*
        유저 닉네임을 기반으로 유저 정보를 검색하는 함수이다。
     */
    public User findByNickname(String nickname) {
        return userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UserNotFoundException("USER_NOT_FOUND_FOR_NICKNAME"));
    }

    /*
        유저의 이름을 업데이트 한다.
     */
    @Transactional  // 변경 사항을 DB에 JPA가 자동으로 업데이트 한다.
    public void updateNickname(String email, String nickname) {
        User user = findByEmail(email);  // 이메일로 유저 정보를 검색한다.

        // 같은 닉네임이면 아무 작업도 하지 않는다.
        if (nickname.equals(user.getNickname())) {
            return;
        }

        // 사전에 닉네임의 중복 검사를 한다.
        if (userRepository.existsByNickname(nickname)) {
            throw new DuplicateNicknameException("DUPLICATE_NICKNAME");
        }

        user.updateNickname(nickname);  // 유저의 닉네임을 업데이트 한다.

        if (user.getUserStatus() == UserStatus.NEW) {  // 유저가 첫 로그인 후 닉네임을 변경하면 active 상태가 된다.
            user.userToActive(); // NEW → ACTIVE
        }
    }

    /*
        유저 프로필 사진을 등록한다.
        사진을 주지 않으면 등록하지 않고, 기존에 등록된 사진이 있다면 삭제한다.
     */
    @Transactional
    public void updateProfileImage(String email, MultipartFile image) {
        User user = findByEmail(email);

        if (image == null || image.isEmpty()) {  // 이미지가 없는 경우이다.
            throw new CloudinaryUploadException("EMPTY_IMAGE");
        }

        // 유저에게 기존 프로필 이미지가 있는지 검색하고 있다면 클라우디너리에서 삭제한다.
        UserProfilePicture userProfilePicture = findByUserIdForProfile(user.getId());
        if (userProfilePicture != null) {  // 이미지가 있어야 동작한다.
            cloudinaryService.deleteImage(userProfilePicture.getCloudinaryPublicId());  // 클라우디너리에서 삭제한다.
        }

        // 클라이언트에게 받은 이미지를 업로드하고 정보를 반환받는다.
        ImageUploadResult imageUploadResult = cloudinaryService.uploadImage(image);

        // 사진 정보를 DB에 반영한다.
        if (userProfilePicture == null) {  // 사진이 없는 상태에서 등록하는 경우이다.
            userProfilePictureRepository.save(UserProfilePicture.builder()
                    .user(user)
                    .cloudinaryPublicId(imageUploadResult.publicId())
                    .imageUrl(imageUploadResult.secureUrl())
                    .build());
        }
        else {  // 사진이 있던 상태에서 등록하는 경우이다.
            userProfilePicture.update(imageUploadResult.publicId(), imageUploadResult.secureUrl());
        }
    }

    /*
        유저 프로필 사진을 삭제하기 위해 사용하는 함수이다.
     */
    @Transactional
    public void removeProfileImage(String email) {
        User user = findByEmail(email);

        UserProfilePicture userProfilePicture = findByUserIdForProfile(user.getId());
        if (userProfilePicture != null) {  // 이미지가 있어야 동작한다.
            cloudinaryService.deleteImage(userProfilePicture.getCloudinaryPublicId());  // 클라우디너리에서 삭제한다.
            deleteProfilePicture(userProfilePicture);  // 서비스 DB에서 삭제한다.
        }
    }

    /*
        유저의 한 줄 소개를 업데이트한다.
     */
    @Transactional
    public void updateIntroduction(String email, String introduction) {
        User user = findByEmail(email);
        user.updateIntroduction(introduction);
    }

    /*
        유저 정보를 삭제한다.
     */
    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    /*
        유저 id로 프로필 사진 정보를 검색한다.
        사진을 검색하지 못해도 정상 상태이다.
     */
    public UserProfilePicture findByUserIdForProfile(Long userId) {
        return userProfilePictureRepository.findByUser_Id(userId)
                .orElse(null);
    }

    /*
        유저 프로필 사진 객체로 사진 객체를 DB에서 삭제한다.
        반드시 클라우디너리의 사진을 먼저 삭제한 뒤 호출해야 한다.
     */
    public void deleteProfilePicture(UserProfilePicture userProfilePicture) {
        userProfilePictureRepository.delete(userProfilePicture);
    }

    /*
        유저 통계의 유저 fk값을 null로 바꿔주는 함수이다.
        회원 탈퇴 시에 사용한다.
     */
    @Transactional
    public void detachStatistics(User user) {
        userStatisticsRepository.detachUser(user);
    }

    /*
        마이페이지의 유저 정보를 반환하는 함수이다.
     */
    public MyProfileResponse getMyProfile(String email) {
        User user = findByEmail(email);

        UserProfilePicture profilePicture = findByUserIdForProfile(user.getId());

        return MyProfileResponse.builder()
                .nickname(user.getNickname())
                .introduction(user.getIntroduction())
                .profileImageUrl(  // 프로필 사진 객체가 null이면 Url을 반환하지 않는다.
                        profilePicture != null ? profilePicture.getImageUrl() : null
                )
                .build();
    }

}
