// 정성우

package me.jsu.springbootdeveloper.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
    관리자 회원가입 시 필요한 이메일 인증 도메인이다.
    해당 이메일로 코드를 보낸 뒤 검증을 받고, 다른 관리자가 승인한다면 최종적으로 회원가입이 완료된다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminEmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;  // 보낼 대상자의 이메일이다.

    @Column(nullable = false)
    private String code;  // 보낸 코드이다.

    @Column(nullable = false)
    private LocalDateTime expiresAt;  // 만료 시간이다.

    public AdminEmailVerification(String email, String code, LocalDateTime expiresAt) {
        this.email = email;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
