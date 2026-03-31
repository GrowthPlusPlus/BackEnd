// 정성우

package me.jsu.springbootdeveloper.user.domain;

import jakarta.persistence.*;
import lombok.*;
import me.jsu.springbootdeveloper.challenge.domain.*;
import me.jsu.springbootdeveloper.post.domain.PostPictureTemporary;
import me.jsu.springbootdeveloper.token.domain.RefreshToken;
import me.jsu.springbootdeveloper.notification.domain.Notification;
import me.jsu.springbootdeveloper.tag.domain.TagToUser;
import me.jsu.springbootdeveloper.user.enumType.Role;
import me.jsu.springbootdeveloper.user.enumType.UserStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import me.jsu.springbootdeveloper.user.dto.UserResponse;

import static jakarta.persistence.CascadeType.REMOVE;

/*
    DB에 저장될 유저 정보를 저장하는 클래스이다.
 */
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(  // 유저 이메일은 서버에 단 하나로 유니크하게 취급하며, 이메일 검색을 인덱싱 한다.
                        name = "uk_user_email",
                        columnNames = {"email"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_delete_user",  // 회원탈퇴한 유저를 검색하기 위한 인덱싱이다.
                        columnList = "status, deleted_at")
        }
)
@EntityListeners(AuditingEntityListener.class)  // 생성 및 수정시간을 감시하고 기록할 수 있게 해준다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class User implements UserDetails {  // userDetails를 상속 받아서 인증 객체로 사용한다.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password")  // OAuth 사용자는 사용하지 않는다.
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;  // 유저 별 권한 지정을 위한 enum이다.

    @Column(name = "nickname", unique = true)
    private String nickname;  // 사용자의 이름이다.

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus userStatus;  // 유저의 상태를 나타내기 위한 enum이다.

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;  // 유저의 한 줄 소개이다.

    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;  // Firebase Cloud Messaging 토큰이다. 푸시 알림 발송 시 사용한다.

    /*
        친구 관련 푸시 알림 활성화 여부를 설정하는 함수이다.
     */
    @Setter
    @Column(name = "friend_push_notification_enabled", nullable = false)
    private boolean friendPushNotificationEnabled = true;  // 친구 관련 푸시 알림 활성화 여부이다. 기본값은 true이다.

    //전체 푸시 알림 활성화 여부 설정 함수
    //전체 푸시 알림 활성화 여부
    @Setter
    @Column(name = "all_push_notification_enabled", nullable = false)
    private boolean allPushNotificationEnabled = true;

    /*
        전체 멤버 인증 푸시 알림 활성화 여부를 설정하는 함수이다.
     */
    //전체 멤버 인증 푸시 알림 활성화 여부
    @Setter
    @Column(name = "all_member_certification_push_notification_enabled", nullable = false)
    private boolean allMemberCertificationPushNotificationEnabled = true;

    //전체 좋아요 푸시알림 활성화 여부
    @Setter
    @Column(name = "all_likes_push_notification_enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean allLikesPushNotificationEnabled = true;

    //전체 댓글 푸시알림 활성화 여부
    @Setter
    @Column(name = "all_comments_push_notification_enabled", nullable = false, columnDefinition = "boolean default true")
    private boolean allCommentsPushNotificationEnabled = true;

    /*
        챌린지 초대 푸시 알림 활성화 여부를 설정하는 함수이다.
     */
    //챌린지 초대 푸시 알림 활성화 여부
    @Setter
    @Column(name = "challenge_invite_push_notification_enabled", nullable = false)
    private boolean challengeInvitePushNotificationEnabled = true;

    //동기부여 메시지 푸시 알림 활성화 여부
    @CreatedDate  // 유저가 생성된 시간이다.
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // 유저가 회원탈퇴한 시각이다.

    /*
        다음 코드는 CASCADE 설정으로 User 엔티티를 fk값으로 가지고 있는 DB들을 자동으로 삭제해주는 설정이다.
        회원탈퇴 등의로 유저 정보가 삭제될 때, 밑의 정보들도 함께 전부 삭제된다.
     */
    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<ChallengeInviteLink> challengeInviteLinks;

    @OneToMany(mappedBy = "inviter", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<ChallengeInvite> sentInvites;

    @OneToMany(mappedBy = "invitee", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private List<ChallengeInvite> receivedInvites;

    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<ChallengeWeeklyStatus> challengeWeeklyStatus;

    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<ChallengeParticipant> challengeParticipants;

    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<TagToUser> tagToUsers;

    // 친구 요청을 양방향으로 매핑한다.
    @OneToMany(mappedBy = "fromUser", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<FriendRequest> sentRequests;

    @OneToMany(mappedBy = "toUser", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<FriendRequest> receivedRequests;

    // 친구 관계를 양방향으로 매핑한다.
    @OneToMany(mappedBy = "user1", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<Friendship> friendshipsAsUser1;

    @OneToMany(mappedBy = "user2", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<Friendship> friendshipsAsUser2;

    @OneToOne(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private UserProfilePicture userProfilePicture;

    @OneToMany(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private List<PostPictureTemporary> postPictureTemporaries;

    @OneToOne(mappedBy = "user", cascade = REMOVE, fetch = FetchType.LAZY)
    private RefreshToken refreshToken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<ChallengeCalenderShare> challengeCalenderShares;

    @Builder
    public User(String email, String password, Role role, String nickname) {
        this.email = email;
        this.password = password;
        this.role = role;  // 회원가입 시 role(권한)이 추가된다.
        this.nickname = nickname;  //OAuth 로그인 시, 구글 프로필에 사용된 이름이 저장된다.
        this.userStatus = UserStatus.NEW;  // 처음 생성시엔 초기 설정을 위해 NEW 상태가 된다.
        this.introduction = "";  // 초기 상태엔 소개가 비어있다.
    }
    
    /*
        사용자 이름을 변경해주는 함수로, OAuth 사용 시 최초 로그인에는 무조건 OAuth 프로필의 닉네임이 들어가므로,
        닉네임을 수정하고 싶다면 해당 함수를 사용해야 한다.
    */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /*
        유저를 active 상태로 변경한다.
     */
    public void userToActive() {
        this.userStatus = UserStatus.ACTIVE;
    }

    /*
        유저가 신고를 당한 경우 제재당한 상태로 변경한다.
     */
    public void userToSuspend() {
        this.userStatus = UserStatus.SUSPENDED;
    }

    /*
        유저를 탈퇴 상태로 변경하고 시각을 기록한다..
     */
    public void userToDelete(LocalDateTime deletedAt) {
        this.userStatus = UserStatus.DELETE;
        this.deletedAt = deletedAt;
    }

    /*
        유저가 로그인하여 회원탈퇴를 취소한 경우, 삭제 날짜를 초기화 하는 함수이다.
     */
    public void clearDeletedAt() {
        this.deletedAt = null;
    }

    /*
        유저의 소개란을 업데이트하는 함수이다.
     */
    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }

    /*
        유저의 FCM 토큰을 업데이트하는 함수이다.
        앱에서 FCM 토큰을 받아 서버에 저장할 때 사용한다.
     */
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /*
        유저의 FCM 토큰을 삭제하는 함수이다.
        로그아웃 시 사용한다.
     */
    public void clearFcmToken() {
        this.fcmToken = null;
    }


    /*
        별도로 관리되는 UserProfilePicture 객체를 통해 이미지 URL을 가져옵니다.
        사진이 없을 경우 null을 반환합니다.
     */
    public String getProfileImageUrl() {
        return (this.userProfilePicture != null) ? this.userProfilePicture.getImageUrl() : null;
    }

    /*
        User 엔티티를 UserResponse DTO로 변환하는 함수이다.
        @return UserResponse DTO 객체
     */
    public UserResponse toResponse() {
        return new UserResponse(this);
    }

    /*
        GrantedAuthority는 ROLE 또는 권한 문자열 하나를 저장하는 인터페이스다.
        SimpleGrantedAuthority로 user라는 권한을 가지는 정보 객체를 생성한다.
        List.of로 준 이유는 스프링 시큐리티는 권한이 여러 개일 수 있다는 전제 설계되었기 때문에 단일 권한을 갖더라도 컬렉션으로 반환한다.

        기본적으로 유저는 ROLE_USER 권한을 갖는다.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.role.name()));  // 스프링 시큐리티에게 이 유저가 가진 권한이 무엇인지 알려주는 함수이다.
    }

    /*
        스프링 시큐리티는 보안을 위해 엔티티의 @Getter 함수를 사용하지 않는다.
        스프링 시큐리티가 비밀번호와 사용자명을 인식하는 기준은 오직 UserDetails의 인터페이스의 함수만 사용한다.
        즉, UserDetails의 getUsername(), getPassword() (Spring Security 인증용 함수)를 유저가 직접 override하여 구현한 함수를 쓴다.

        OAuth 로그인에서는 사용하지 않고, 우리 서비스에 직접 회원가입한 유저의 경우에만 사용하는 인증이다.
        단, 로그인에선 사용하지 않지만, PostApiController처럼 게시글을 올리는 경우 스프링 시큐리티가 사용하기도 한다.
     */
    @Override
    public String getUsername() {
        return email;
    }
    @Override
    public String getPassword() {
        return password;
    }

    /*
        1. 아직 계정 상태 체크 로직을 사용하지 않는다.
        2. 계정은 모두 정상(active)이라고 가정한다.
        3. 관리 기능이 없다면 true를 반환하는 것이 가장 일반적이다.
     */
    @Override
    public boolean isAccountNonExpired() {   // 계정 만료 여부를 반환하는 함수이다.
        // 만료되었는지 확인하는 로직
        return true;  // true면 만료되지 않았음을 의미한다.
    }
    @Override
    public boolean isAccountNonLocked() {  // 계정 잠금 여부를 반환하는 함수이다.
        // 계정 잠금되었는지 확인하는 로직
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {  // 패스워드 만료 여부를 반환하는 함수이다.
        return true;
    }
    @Override
    public boolean isEnabled() {  // 계정 사용 가능 여부를 반환하는 함수이다.
        // 계정이 아직 사용 가능한지 확인하는 로직
        return true;
    }
}
