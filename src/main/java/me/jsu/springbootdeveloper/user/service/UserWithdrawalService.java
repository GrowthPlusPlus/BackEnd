// 정성우

package me.jsu.springbootdeveloper.user.service;

import me.jsu.springbootdeveloper.challenge.service.ChallengeDeleteService;
import me.jsu.springbootdeveloper.post.service.ReportService;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.domain.UserProfilePicture;
import me.jsu.springbootdeveloper.challenge.service.ChallengeService;
import me.jsu.springbootdeveloper.cloudinary.service.CloudinaryService;
import me.jsu.springbootdeveloper.post.service.CommentService;
import me.jsu.springbootdeveloper.post.service.PostLikeService;
import me.jsu.springbootdeveloper.post.service.PostService;
import org.springframework.stereotype.Service;

/*
    유저가 회원탈퇴를 하는 경우, 리프레시 토큰과 유저 정보를 삭제해주는 서비스 클래스이다.
 */
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final ChallengeDeleteService challengeDeleteService;
    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final CloudinaryService cloudinaryService;
    private final ReportService reportService;

    @Transactional
    public void withdraw(String email) {
        User user = userService.findByEmail(email);

        // 유저가 챌린지 오너인 경우, 다른 사용자에게 위임한다.
        challengeDeleteService.handleOwnerWithdrawal(user.getId());

        // 게시글, 로그, 댓글, 좋아요, 유저 통계 등의 정보는 FK를 해제하여 회원이 사라져도 남겨두도록 한다.
        challengeDeleteService.detachLogToUser(user);  // 로그 해제
        postService.detachPostToUser(user);  // 게시글 해제
        commentService.detachCommentToUser(user);  // 댓글 해제
        postLikeService.detachLikeToUser(user);  // 좋아요 해제
        userService.detachStatistics(user);  // 통계 해제
        reportService.detachReportArticleToUser(user.getId());  // 유저의 인증글 신고 해제
        reportService.detachReportCommentToUser(user.getId());  // 유저의 댓글 신고 해제

        // 참여한 챌린지에서 나온다. CASCADE에 의존하지 않고 직접 구현한 챌린지 서비스의 삭제를 사용한다.
        challengeDeleteService.deleteParticipant(user.getId());

        /*
            클라우디너리의 사진인 외부 리소스를 삭제한다.
            삭제를 위해 DB에서 유저 프로필 사진 객체를 검색한다.
         */
        UserProfilePicture userProfilePicture = userService.findByUserIdForProfile(user.getId());
        if (userProfilePicture != null) {  // 사진이 존재하면 삭제한다.
            cloudinaryService.deleteImage(userProfilePicture.getCloudinaryPublicId());  // public id로 삭제한다.
        }

        // 유저의 리프레시 토큰을 직접 삭제한다.
        refreshTokenService.deleteByUser_Id(user.getId());

        // 유저를 삭제한다. 유저가 사라짐에 따라 필요 없는 정보들은 CASCADE로 자동으로 삭제된다.
        userService.deleteUser(user);
    }
}
