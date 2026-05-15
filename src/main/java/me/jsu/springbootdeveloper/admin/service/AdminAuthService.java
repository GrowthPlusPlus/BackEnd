// 정성우

package me.jsu.springbootdeveloper.admin.service;

import com.resend.Resend;
import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.admin.domain.AdminEmailVerification;
import me.jsu.springbootdeveloper.admin.dto.AdminLoginResponse;
import me.jsu.springbootdeveloper.admin.exception.*;
import me.jsu.springbootdeveloper.admin.repository.AdminEmailVerificationRepository;
import me.jsu.springbootdeveloper.config.jwt.TokenProvider;
import me.jsu.springbootdeveloper.config.resend.ResendProperties;
import me.jsu.springbootdeveloper.fcm.service.FcmService;
import me.jsu.springbootdeveloper.token.service.RefreshTokenService;
import me.jsu.springbootdeveloper.user.domain.User;
import me.jsu.springbootdeveloper.user.enumType.Role;
import me.jsu.springbootdeveloper.user.enumType.UserStatus;
import me.jsu.springbootdeveloper.user.repository.UserRepository;
import me.jsu.springbootdeveloper.user.service.UserService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

/*
    관리자 회원가입/로그인 기능을 수행하는 서비스이다.
 */
@RequiredArgsConstructor
@Service
public class AdminAuthService {
    private final JavaMailSender javaMailSender;
    private final ResendProperties resendProperties;
    private final AdminEmailVerificationRepository verificationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final FcmService fcmService;
    private final UserService userService;
    private final UserRepository userRepository;

    @Transactional
    public void adminSignup(String email, String password, String passwordVerify, String nickname) {
        userService.existsByEmail(email);  // 이미 가입된 이메일이 있는지 검사한다. 있다면 예외처리한다.

        // 비밀번호가 일치하는지 검사한다.
        if (!password.equals(passwordVerify)) {
            throw new AdminAuthUnauthorizedException("비밀번호가 일치하지 않습니다.");
        }

        // 패스워드는 DB에 저장할 땐 암호화 한다.
        String encodedPassword = passwordEncoder.encode(password);

        // 관리자 계정을 일단 생성한다.
        User admin = User.builder()
                .email(email)
                .password(encodedPassword)
                .role(Role.ROLE_ADMIN)
                .nickname(nickname)
                .build();

        admin.adminEmailPending();  // 이메일 대기 상태로 만든다.
        userRepository.save(admin);  // DB에 업데이트 한다.

        sendVerificationCodeMake(email);  // 이메일 코드를 보낸다.
    }

    /*
        관리자 권한으로 로그인하는 함수이다.
     */
    @Transactional
    public AdminLoginResponse adminLogin(String email, String password, String fcmToken) {
        User admin = userService.findByEmail(email);

        // 관리자 계정 이메일인지 검사한다.
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AdminAuthForbiddenException("관리자 계정이 아닙니다.");
        }

        // 비밀번호가 일치하는지 검사한다.
        if (!passwordEncoder.matches(password, admin.getPassword())) {
            throw new AdminAuthUnauthorizedException("비밀번호가 일치하지 않습니다.");
        }

        // 이메일 인증을 했는지 검사한다.
        if (admin.getUserStatus() == UserStatus.ADMIN_EMAIL_PENDING) {
            throw new AdminAuthUnauthorizedException("이메일 인증이 필요합니다.");
        }

        // 관리자 승인을 받았는지 검사한다.
        if (admin.getUserStatus() == UserStatus.ADMIN_APPROVAL_PENDING) {
            throw new AdminAuthForbiddenException("관리자 승인이 필요합니다.");
        }

        // 관리자 승인을 받고 활성화된 계정인지 검사한다.
        if (admin.getUserStatus() != UserStatus.ACTIVE) {
            throw new AdminAuthForbiddenException("활성화된 계정이 아닙니다.");
        }

        // 관리자의 토큰을 생성한다.
        String accessToken = tokenProvider.generateToken(admin, Duration.ofHours(1));
        String refreshToken = tokenProvider.generateToken(admin, Duration.ofDays(30));

        // Refresh Token을 우리 서비스의 서버에 저장한다.
        refreshTokenService.saveForOAuth(admin, refreshToken);

        /*
            FCM 토큰이 제공된 경우, 로그인 성공 시 자동으로 등록한다.
            FCM 토큰은 앱에서 Firebase SDK를 통해 생성되며, 로그인 요청과 함께 전달될 수 있다.
            프론트엔드 개발자 편의를 위해 로그인 시 FCM 토큰을 함께 보낼 수 있도록 옵셔널로 지원한다.
         */
        boolean fcmTokenRegistered = false;
        if (fcmToken != null && !fcmToken.isBlank()) {
            try {
                fcmService.registerOrUpdateToken(admin.getEmail(), fcmToken);
                fcmTokenRegistered = true;
            } catch (Exception e) {
                // FCM 토큰 등록 실패해도 로그인은 성공으로 처리한다.
                // FCM 토큰은 나중에 별도 API로 등록할 수 있으므로 로그인 실패로 처리하지 않는다.
                // 로그만 남기고 계속 진행한다.
            }
        } else {
            // FCM 토큰이 로그인 요청에 포함되지 않은 경우, 기존에 등록된 토큰이 있는지 확인한다.
            fcmTokenRegistered = (admin.getFcmToken() != null && !admin.getFcmToken().isBlank());
        }

        // 관리자에게 토큰을 반환한다.
        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fcmTokenRegistered(fcmTokenRegistered)
                .build();
    }

    /*
        유저가 받은 이메일 코드를 인증하는 함수이다.
     */
    @Transactional
    public void verifyAdminEmail(String email, String code) {
        User admin = userService.findByEmail(email);

        // 이메일 인증하려는 계정이 관리자가 아닌경우 예외처리한다.
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AdminAuthForbiddenException("관리자 계정이 아닙니다.");
        }

        // 인증하려는 이메일이 DB 정보에 존재하지 않는다면 예외처리한다.
        AdminEmailVerification verification = verificationRepository.findByEmail(email)
                .orElseThrow(() -> new AdminAuthNotFoundException("인증 코드가 없습니다."));

        // 인증 코드가 만료되지 않았는지 검사한다.
        if (verification.isExpired()) {
            throw new AdminAuthConflictException("인증 코드가 만료되었습니다.");
        }

        // 우리 서버의 인증코드와 유저가 보낸 인증코드가 일치하는지 검사한다.
        if (!verification.getCode().equals(code)) {
            throw new AdminAuthUnauthorizedException("인증 코드가 일치하지 않습니다.");
        }

        admin.adminApprovalPending();  // 해당 계정을 관리자 승인 대기 상태로 만든다.
        verificationRepository.deleteByEmail(email);  // 인증 이메일들을 삭제한다.
    }

    /*
        유저에게 보낼 code를 만드는 함수이다.
     */
    private void sendVerificationCodeMake(String email) {
        verificationRepository.deleteByEmail(email);  // 기존에 있던 해당 이메일의 인증 정보를 모두 삭제한다.

        String code = generateCode();  // 코드를 랜덤하게 생성한다.

        AdminEmailVerification verification = new AdminEmailVerification(
                email,
                code,
                LocalDateTime.now().plusMinutes(5)  // 만료시간을 5분으로 한다.
        );

        verificationRepository.save(verification);  // 인증 메일을 저장한다.
        sendVerificationCodeResend(email, code);  // 실제로 이메일에 보내준다.
    }

    private String generateCode() {
        Random random = new Random();
        int number = 100000 + random.nextInt(900000);
        return String.valueOf(number);
    }

    /*
        관리자 회원가입 시, 이메일로 인증 코드를 보내주는 함수이다.
        해당 방식은 구글을 이용하는 방식으로 2단계 인증을 하지 않으면 사용할 수 없다.
     */
    public void sendVerificationCodeGoogle(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);  // 보낼 대상자의 이메일이다.
        message.setSubject("[해냄] 관리자 이메일 인증 코드");
        message.setText(
                "관리자 회원가입을 위한 이메일 인증 코드입니다.\n\n" +
                        "인증 코드: " + code + "\n\n" +
                        "인증 코드는 5분 동안 유효합니다."
        );

        javaMailSender.send(message);  // 실제로 보낸다.
    }

    /*
        관리자 회원가입 시, 이메일로 인증 코드를 보내주는 함수이다.
        해당 방식은 resend를 이용하는 방식으로 돈을 내지 않으면 관리자 외의 이메일에는 보낼 수 없다.
     */
    public void sendVerificationCodeResend(String email, String code) {
        try {
            Resend resend = new Resend(resendProperties.apiKey());

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(resendProperties.from())
                    .to(email)
                    .subject("[해냄] 관리자 이메일 인증 코드")
                    .html(
                            "<h2>관리자 이메일 인증 코드</h2>" +
                                    "<p>관리자 회원가입을 위한 인증 코드입니다.</p>" +
                                    "<h1>" + code + "</h1>" +
                                    "<p>인증 코드는 5분 동안 유효합니다.</p>"
                    )
                    .build();

            resend.emails().send(params);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new AdminAuthBadRequestException("ADMIN_EMAIL_SEND_FAILED");
        }
    }

    /*
        인증 코드를 재발송하는 함수이다.
     */
    @Transactional
    public void resendVerificationCode(String email) {
        User admin = userService.findByEmail(email);  // 코드를 다시 보낼 유저의 이메일을 가져온다.

        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AdminAuthForbiddenException("관리자 계정이 아닙니다.");
        }

        // 이미 이메일 인증한 계정에 다시 code를 보내려는 경우에 예외처리한다.
        if (admin.getUserStatus() != UserStatus.ADMIN_EMAIL_PENDING) {
            throw new AdminAuthBadRequestException("이메일 인증 대기 상태가 아닙니다.");
        }

        sendVerificationCodeMake(email);  // 기존 전송 방식으로 재전송한다.
    }

    /*
        관리자 권한으로 회원가입한 계정을 사용할 수 있게 다른 관리자가 승인해주는 함수이다.
     */
    @Transactional
    public void approveAdmin(Long userId) {
        User admin = userService.findById(userId);  // 승인하려는 유저의 정보를 가져온다.

        // 승인하려는 유저가 관리자 계정으로 가입된 것이 아니라면 예외처리한다.
        if (admin.getRole() != Role.ROLE_ADMIN) {
            throw new AdminAuthForbiddenException("관리자 계정이 아닙니다.");
        }

        // 승인하려는 유저의 계정이 이메일을 인증받은 후 관리자 승인 대기 상태가 아니라면 예외처리한다.
        if (admin.getUserStatus() != UserStatus.ADMIN_APPROVAL_PENDING) {
            throw new AdminAuthBadRequestException("승인 대기 상태가 아닙니다.");
        }

        admin.userToActive();  // 관리자의 상태를 ACTIVE로 만들어 로그인할 수 있게 한다.
    }
}
