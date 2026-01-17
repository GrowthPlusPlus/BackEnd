// 정성우

package me.jsu.springbootdeveloper.repository.user;

import me.jsu.springbootdeveloper.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/*
    유저 정보를 저장하는 DB로, 테이블에 매핑되는 엔티티 타입은 User, 엔티티의 pk 타입은 Long이다.
    findByAll 검색 방식 이외에도 유저 이메일로 조회가 가능하다.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);  //이메일 정보로 사용자 정보를 가져올 수 있게 하는 쿼리문을 작성 가능하게 함
    Optional<User> findByNickname(String nickname); // 닉네임 정보로 사용자 정보를 가져올 수 있게 하는 쿼리문을 작성 가능하게 함
}
