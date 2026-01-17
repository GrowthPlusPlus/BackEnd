package me.jsu.springbootdeveloper.controller;

import lombok.RequiredArgsConstructor;
import me.jsu.springbootdeveloper.domain.user.User;
import me.jsu.springbootdeveloper.dto.UserResponse;
import me.jsu.springbootdeveloper.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/*
    해당 클래스는 REST API 방식으로 유저 정보를 검색하는 기능을 제공한다.
    최초 작성자: 정성우
 */
@RequiredArgsConstructor
@RestController
public class UserApiController {

    private final UserService userService;

    /*
        닉네임을 통해 유저 정보를 검색하는 GET 요청 API이다.
        @param nickname 검색할 유저의 닉네임
        @return 검색된 유저 정보 (UserResponse 객체)
     */
    @GetMapping("/api/users/nickname/{nickname}")
    public ResponseEntity<UserResponse> findUserByNickname(@PathVariable String nickname) {
        User user = userService.findByNickname(nickname);
        return ResponseEntity.ok(user.toResponse());
    }

    /*
        이메일을 통해 유저 정보를 검색하는 GET 요청 API이다.
        @param email 검색할 유저의 이메일
        @return 검색된 유저 정보 (UserResponse 객체)
     */
    @GetMapping("/api/users/email/{email}")
    public ResponseEntity<UserResponse> findUserByEmail(@PathVariable String email) {
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(user.toResponse());
    }
}
