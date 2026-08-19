// 정성우

package me.jsu.springbootdeveloper.user.service;

import me.jsu.springbootdeveloper.challenge.service.ChallengeDeleteService;
import me.jsu.springbootdeveloper.post.service.ReportService;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import me.jsu.springbootdeveloper.user.enumType.UserStatus;
import me.jsu.springbootdeveloper.user.exception.NotAllowedAdminException;
import me.jsu.springbootdeveloper.user.repository.UserProfilePictureRepository;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.repository.UserStatisticsRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.post.service.CommentService;
import me.jsu.springbootdeveloper.post.service.PostLikeService;
import me.jsu.springbootdeveloper.post.service.PostService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/*
    유저가 회원탈퇴를 하는 경우, 리프레시 토큰과 유저 정보를 삭제해주는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserStatisticsRepository userStatisticsRepository;
    private final UserProfilePictureRepository userProfilePictureRepository;
    private final RefreshTokenService refreshTokenService;
    private final ChallengeDeleteService challengeDeleteService;
    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;

    /*
        유저가 회원탈퇴를 하면 일시적으로 유저의 상태를 탈퇴로 만들고, 30일 내에 로그인 하지 않으면 삭제되도록 한다.
        참여 중인 챌린지 중, 방장인 경우엔 위임하고 다른 정보는 그대로 유지한다.
     */
    @Transactional
    public void unregister(String email) {
        User user = userService.findByEmail(email);

        // 유저가 챌린지 오너인 경우, 다른 사용자에게 위임한다.
        challengeDeleteService.handleOwnerWithdrawal(user.getId());

        // 유저의 상태를 탈퇴 상태로 변경한다.
        LocalDateTime today = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        user.userToDelete(today);

        // 유저의 리프레시 토큰을 직접 삭제한다.
        refreshTokenService.deleteByUser_Id(user.getId());

        // 임시
        withdraw(today.plusMinutes(1));
    }

    /*
        30일의 유예 기간이 지났을 때, 탈퇴 상태인 유저를 제거하도록 스케줄러가 호출하는 함수이다. 유저마다 정보를 삭제한다.
     */
    @Transactional
    public void withdraw(LocalDateTime threshold) {
        List<User> users = userRepository.findUsersToHardDelete(UserStatus.DELETE, threshold);

        // 쿼리문을 줄이기 위해 유저 id를 추출한다.
        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        // 게시글, 댓글, 좋아요, 유저 통계 등의 정보는 FK를 해제하여 회원이 사라져도 남겨두도록 한다.
        //challengeDeleteService.detachLogToUser(userIds);  // 로그 해제
        postService.detachPostToUser(userIds);  // 게시글 해제
        commentService.detachCommentToUser(userIds);  // 댓글 해제
        postLikeService.detachLikeToUser(userIds);  // 좋아요 해제
        userStatisticsRepository.detachUser(userIds);  // 통계 해제
        reportService.detachReportArticleToUser(userIds);  // 유저의 인증글 신고 해제
        reportService.detachReportCommentToUser(userIds);  // 유저의 댓글 신고 해제

        // 참여한 챌린지에서 나온다. CASCADE에 의존하지 않고 직접 구현한 챌린지 서비스의 삭제를 사용한다.
        challengeDeleteService.deleteParticipant(userIds);

        /*
            클라우디너리의 사진인 외부 리소스를 삭제한다.
            삭제를 위해 DB에서 유저 프로필 사진 객체를 검색한다.
         */
        List<UserProfilePicture> userProfilePictures = userProfilePictureRepository.findByUserIds(userIds);
        for (UserProfilePicture userProfilePicture : userProfilePictures) {  // 사진이 존재하면 삭제한다.
            cloudinaryService.deleteImage(userProfilePicture.getCloudinaryPublicId());  // public id로 삭제한다.
        }

        // 유저를 삭제한다. 유저가 사라짐에 따라 필요 없는 정보들은 CASCADE로 자동으로 삭제된다.
        // userRepository.deleteAllByIdInBatch(userIds); batch작업을 수행하면 cascade가 적용되지 않는다.
        userRepository.deleteAll(users);
    }

    /*
        관리자가 특정 유저를 유예 없이 즉시 삭제할 때 사용한다.
        스케줄러를 통한 유저 삭제와 동일한 함수를 사용하므로, 현재는 특정 유저 삭제 시 회원 탈퇴 후 유예일이 지난 유저도 함께 삭제된다.

        @param userId 삭제 대상 유저 id
        @param adminEmail 요청 관리자 이메일(JWT sub)
     */
    @Transactional
    public void adminHardDeleteUser(Long userId, String adminEmail) {
        User admin = userService.findByEmail(adminEmail);
        User target = userService.findById(userId);

        if (admin.getId().equals(target.getId())) {  // 어드민은 스스로 삭제할 수 없다.
            throw new NotAllowedAdminException("ADMIN_CANNOT_DELETE_SELF");
        }

        // 유저가 챌린지 오너인 경우, 다른 사용자에게 위임한다.
        challengeDeleteService.handleOwnerWithdrawal(target.getId());

        LocalDateTime today = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        // 유저의 상태를 탈퇴 상태로 변경한다. 즉시 삭제를 위해 50일 이전으로 설정한다.
        LocalDateTime deleteAt = today.minusDays(50);
        target.userToDelete(deleteAt);

        // 유저의 리프레시 토큰을 직접 삭제한다.
        refreshTokenService.deleteByUser_Id(target.getId());

        withdraw(today);  // 유저를 삭제한다.
    }
}
