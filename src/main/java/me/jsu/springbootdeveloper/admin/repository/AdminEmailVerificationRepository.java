// 정성우

package me.jsu.springbootdeveloper.admin.repository;

import me.jsu.springbootdeveloper.admin.domain.AdminEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    이메일 인증을 위해 사용하는 리포지토리이다.
 */
public interface AdminEmailVerificationRepository extends JpaRepository<AdminEmailVerification, Long> {
    Optional<AdminEmailVerification> findByEmail(String email);  // 인증 이메일의 code값을 가져오는 쿼리문이다.
    void deleteByEmail(String email);  // 이메일에 해당하는 인증 코드를 모두 삭제하는 쿼리문이다.
}
