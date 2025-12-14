package me.jsu.springbootdeveloper.repository;

import me.jsu.springbootdeveloper.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);  //이메일 정보로 사용자 정보를 가져올 수 있게 하는 쿼리문을 작성 가능하게 함
}
